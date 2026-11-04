package com.manish.exam_backend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the plain text of a PDF into multiple-choice questions.
 * It has no Spring code in it, so it is easy to test with simple strings.
 */
public final class PdfQuestionParser {

    public record ParsedQuestion(int number, String text, String optionA, String optionB,
                                 String optionC, String optionD, String correctOption) {}

    public record ParseResult(List<ParsedQuestion> questions, List<String> warnings) {}

    private static final int MAX_QUESTIONS = 300;

    // The start of a question line: "1." "1)" "Q1." "Q.1)" "Question 1:"
    private static final Pattern QUESTION_START = Pattern.compile(
            "(?im)^[ \\t]*(?:q(?:uestion)?[ \\t]*\\.?[ \\t]*)?(\\d{1,3})[ \\t]*[.):][ \\t]+(?=\\S)");

    // A heading line that only says "Answer Key", "Answers" or "Solutions"
    private static final Pattern KEY_HEADING = Pattern.compile(
            "(?im)^[ \\t]*(?:answer[ \\t]*key|answers|solutions?)[ \\t]*:?[ \\t]*$");

    // One entry of an answer key: "1-A", "2. C", "3) b"
    private static final Pattern KEY_ENTRY = Pattern.compile(
            "(\\d{1,3})[ \\t]*[-.):=\\u2013]?[ \\t]*\\(?([A-Da-d])\\)?(?![A-Za-z0-9])");

    // The option letters: "A)" "A." "(A)"
    private static final Pattern OPTION_UPPER = Pattern.compile(
            "(?<![A-Za-z0-9])\\(?([A-D])[.)]\\s");
    private static final Pattern OPTION_ANY = Pattern.compile(
            "(?<![A-Za-z0-9])\\(?([A-Da-d])[.)]\\s");

    // The answer line inside a question: "Answer: B", "Ans: (c)", "Correct Answer - D"
    private static final Pattern ANSWER = Pattern.compile(
            "(?i)(?<![A-Za-z])(?:correct[ \\t]+)?(?:answer|ans)(?![A-Za-z])[ \\t]*(?:is)?[ \\t]*"
                    + "[:.\\-=)\\u2013]*[ \\t]*(?:option)?[ \\t]*\\(?([A-D])(?![A-Za-z0-9])");

    private PdfQuestionParser() {}

    public static ParseResult parse(String rawText) {
        List<ParsedQuestion> questions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (rawText == null || rawText.isBlank()) {
            return new ParseResult(questions, warnings);
        }

        String text = clean(rawText);

        // If the PDF has an "Answer Key" at the end, read it and cut it off
        Map<Integer, String> key = new HashMap<>();
        int keyStart = -1;
        Matcher keyMatcher = KEY_HEADING.matcher(text);
        while (keyMatcher.find()) {
            keyStart = keyMatcher.start();
        }
        if (keyStart >= 0) {
            Matcher entries = KEY_ENTRY.matcher(text.substring(keyStart));
            while (entries.find()) {
                key.putIfAbsent(Integer.parseInt(entries.group(1)), entries.group(2).toUpperCase());
            }
            text = text.substring(0, keyStart);
        }

        // Find the questions. They must be numbered 1, 2, 3 ... one after the other.
        List<int[]> starts = new ArrayList<>(); // {where it starts, where the text starts, number}
        Matcher qm = QUESTION_START.matcher(text);
        int expected = 1;
        int highestSeen = 0;
        while (qm.find()) {
            int n = Integer.parseInt(qm.group(1));
            highestSeen = Math.max(highestSeen, n);
            if (n == expected) {
                starts.add(new int[] {qm.start(), qm.end(), n});
                expected++;
            }
        }
        if (highestSeen >= expected) {
            warnings.add("The question numbers seem to skip after question " + (expected - 1)
                    + ". Questions after that may be missing. Number them 1, 2, 3 ... in order.");
        }

        int limit = Math.min(starts.size(), MAX_QUESTIONS);
        if (starts.size() > MAX_QUESTIONS) {
            warnings.add("Only the first " + MAX_QUESTIONS + " questions were read.");
        }

        for (int i = 0; i < limit; i++) {
            int bodyStart = starts.get(i)[1];
            int bodyEnd = (i + 1 < starts.size()) ? starts.get(i + 1)[0] : text.length();
            int number = starts.get(i)[2];
            ParsedQuestion q = parseBlock(number, text.substring(bodyStart, bodyEnd), key, warnings);
            if (q != null) {
                questions.add(q);
            }
        }
        return new ParseResult(questions, warnings);
    }

    private static ParsedQuestion parseBlock(int number, String body, Map<Integer, String> key,
                                             List<String> warnings) {
        String flat = body.replaceAll("\\s+", " ").trim();

        int[][] marks = findOptionMarks(flat, OPTION_UPPER);
        if (marks == null) {
            marks = findOptionMarks(flat, OPTION_ANY);
        }
        if (marks == null) {
            warnings.add("Question " + number + ": could not find the options A, B, C and D. Skipped.");
            return null;
        }

        String questionText = flat.substring(0, marks[0][0]).trim();
        String a = flat.substring(marks[0][1], marks[1][0]).trim();
        String b = flat.substring(marks[1][1], marks[2][0]).trim();
        String c = flat.substring(marks[2][1], marks[3][0]).trim();
        String tail = flat.substring(marks[3][1]);

        // The answer line comes after option D
        String correct = null;
        String d = tail;
        Matcher am = ANSWER.matcher(tail);
        if (am.find()) {
            correct = am.group(1).toUpperCase();
            d = tail.substring(0, am.start());
        }
        d = d.trim();
        if (correct == null) {
            correct = key.get(number);
        }

        if (questionText.isEmpty() || a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty()) {
            warnings.add("Question " + number + ": the question or one of its options is empty. Skipped.");
            return null;
        }
        if (correct == null) {
            warnings.add("Question " + number + ": could not find the correct answer. Skipped.");
            return null;
        }
        return new ParsedQuestion(number, questionText, a, b, c, d, correct);
    }

    // Looks for A, B, C, D in that order. Returns {start, end} of each marker, or null.
    private static int[][] findOptionMarks(String flat, Pattern pattern) {
        List<int[]> all = new ArrayList<>(); // {letter, start, end}
        Matcher m = pattern.matcher(flat);
        while (m.find()) {
            all.add(new int[] {Character.toUpperCase(m.group(1).charAt(0)), m.start(), m.end()});
        }
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i)[0] != 'A') {
                continue;
            }
            int[][] chain = new int[4][];
            chain[0] = new int[] {all.get(i)[1], all.get(i)[2]};
            int from = i + 1;
            boolean ok = true;
            for (int letter = 'B'; letter <= 'D'; letter++) {
                int found = -1;
                for (int j = from; j < all.size(); j++) {
                    if (all.get(j)[0] == letter) {
                        found = j;
                        break;
                    }
                }
                if (found < 0) {
                    ok = false;
                    break;
                }
                chain[letter - 'A'] = new int[] {all.get(found)[1], all.get(found)[2]};
                from = found + 1;
            }
            if (ok) {
                return chain;
            }
        }
        return null;
    }

    // Fixes odd characters and removes lines that are only a page number
    private static String clean(String raw) {
        String text = raw.replace('\u00A0', ' ').replace("\u0000", "")
                .replace("\r\n", "\n").replace('\r', '\n').replace('\f', '\n');
        String[] lines = text.split("\n", -1);
        StringBuilder out = new StringBuilder();
        String previous = "";
        for (String line : lines) {
            String trimmed = line.trim();
            boolean pageWord = trimmed.matches("(?i)(page[ \\t]+)?\\d{1,3}[ \\t]+(of|/)[ \\t]+\\d{1,3}")
                    || trimmed.matches("(?i)page[ \\t]+\\d{1,3}")
                    || trimmed.matches("-[ \\t]*\\d{1,3}[ \\t]*-");
            // a lone number is a page number, unless it is the text of an option such as "A)"
            boolean loneNumber = trimmed.matches("\\d{1,3}")
                    && !previous.matches("(?s).*(?<![A-Za-z0-9])\\(?[A-Da-d][.)]\\s*$");
            if (pageWord || loneNumber) {
                continue;
            }
            out.append(line).append('\n');
            if (!trimmed.isEmpty()) {
                previous = trimmed;
            }
        }
        return out.toString();
    }
}
