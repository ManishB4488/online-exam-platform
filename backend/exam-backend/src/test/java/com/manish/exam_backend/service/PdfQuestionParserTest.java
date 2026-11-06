package com.manish.exam_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.manish.exam_backend.service.PdfQuestionParser.ParseResult;

class PdfQuestionParserTest {

    @Test
    void readsQuestionsWithAnswerAfterEachOne() {
        String text = String.join("\n",
                "Java Basics Test",
                "Instructions: choose one answer.",
                "",
                "1. Which keyword is used to create an object in Java?",
                "A) new", "B) class", "C) this", "D) static", "Answer: A",
                "",
                "2. Which of these is NOT a primitive data type?",
                "A) int", "B) boolean", "C) String", "D) double", "Answer: C");

        ParseResult result = PdfQuestionParser.parse(text);

        assertThat(result.warnings()).isEmpty();
        assertThat(result.questions()).hasSize(2);
        assertThat(result.questions().get(0).text())
                .isEqualTo("Which keyword is used to create an object in Java?");
        assertThat(result.questions().get(0).correctOption()).isEqualTo("A");
        assertThat(result.questions().get(1).correctOption()).isEqualTo("C");
    }

    @Test
    void readsOptionsWrittenOnOneLineAndQPrefix() {
        String text = String.join("\n",
                "Q1. What does JVM stand for? It is",
                "used to run Java programs.",
                "A) Java Virtual Machine B) Java Variable Method C) Joint Virtual Memory D) None",
                "Ans: A",
                "Q2) The size of an int is",
                "(A) 8 bits (B) 16 bits (C) 32 bits (D) 64 bits",
                "Correct Answer: C");

        ParseResult result = PdfQuestionParser.parse(text);

        assertThat(result.questions()).hasSize(2);
        assertThat(result.questions().get(0).text())
                .isEqualTo("What does JVM stand for? It is used to run Java programs.");
        assertThat(result.questions().get(0).optionB()).isEqualTo("Java Variable Method");
        assertThat(result.questions().get(1).optionC()).isEqualTo("32 bits");
    }

    @Test
    void readsLowercaseOptionsAndBracketedAnswer() {
        String text = String.join("\n",
                "1. Capital of France?",
                "a. Berlin", "b. Paris", "c. Rome", "d. Madrid",
                "Answer: (b)");

        ParseResult result = PdfQuestionParser.parse(text);

        assertThat(result.questions()).hasSize(1);
        assertThat(result.questions().get(0).correctOption()).isEqualTo("B");
        assertThat(result.questions().get(0).optionB()).isEqualTo("Paris");
    }

    @Test
    void readsAnAnswerKeyAtTheEnd() {
        String text = String.join("\n",
                "1. First question?", "A. one", "B. two", "C. three", "D. four",
                "2. Second question?", "A. red", "B. green", "C. blue", "D. black",
                "3. Third?", "A. x", "B. y", "C. z", "D. w",
                "",
                "Answer Key",
                "1-B 2-C 3. A");

        ParseResult result = PdfQuestionParser.parse(text);

        assertThat(result.questions()).hasSize(3);
        assertThat(result.questions().get(0).correctOption()).isEqualTo("B");
        assertThat(result.questions().get(1).correctOption()).isEqualTo("C");
        assertThat(result.questions().get(2).correctOption()).isEqualTo("A");
        assertThat(result.questions().get(2).optionD()).isEqualTo("w");
    }

    @Test
    void skipsAQuestionWithoutAnAnswerAndGivesAWarning() {
        String text = String.join("\n",
                "1. No answer here?", "A) a", "B) b", "C) c", "D) d",
                "2. Has an answer?", "A) a", "B) b", "C) c", "D) d", "Answer: B");

        ParseResult result = PdfQuestionParser.parse(text);

        assertThat(result.questions()).hasSize(1);
        assertThat(result.questions().get(0).number()).isEqualTo(2);
        assertThat(result.warnings()).hasSize(1);
    }

    @Test
    void warnsWhenTheQuestionNumbersSkip() {
        String text = String.join("\n",
                "1. One?", "A) a", "B) b", "C) c", "D) d", "Answer: A",
                "2. Two?", "A) a", "B) b", "C) c", "D) d", "Answer: A",
                "4. Four?", "A) a", "B) b", "C) c", "D) d", "Answer: A");

        ParseResult result = PdfQuestionParser.parse(text);

        assertThat(result.questions()).hasSize(2);
        assertThat(result.warnings()).hasSize(1);
    }

    @Test
    void removesPageNumbers() {
        String text = String.join("\n",
                "1. Question one?", "A) a", "B) b", "C) c", "D) d", "Answer: A",
                "",
                "Page 1 of 2",
                "2. Question two?", "A) a", "B) b", "C) c", "D) d", "2", "Answer: B",
                "3. Question three?", "A) a", "B) b", "C) c", "D) last option", "", "2", "Answer: C");

        ParseResult result = PdfQuestionParser.parse(text);

        assertThat(result.questions()).hasSize(3);
        assertThat(result.questions().get(2).optionD()).isEqualTo("last option");
    }

    @Test
    void doesNotMixUpTheWordAnswerInsideAnOption() {
        String text = String.join("\n",
                "1. Pick the best answer for 2+2.",
                "A) 3", "B) 4", "C) The answer is five", "D) 22",
                "Answer: B",
                "Explanation: 2+2 is 4.");

        ParseResult result = PdfQuestionParser.parse(text);

        assertThat(result.questions()).hasSize(1);
        assertThat(result.questions().get(0).correctOption()).isEqualTo("B");
        assertThat(result.questions().get(0).optionC()).isEqualTo("The answer is five");
        assertThat(result.questions().get(0).optionD()).isEqualTo("22");
    }

    @Test
    void textWithoutQuestionsGivesAnEmptyResult() {
        assertThat(PdfQuestionParser.parse("Just some notes\nwithout questions").questions()).isEmpty();
        assertThat(PdfQuestionParser.parse("").questions()).isEmpty();
        assertThat(PdfQuestionParser.parse(null).questions()).isEmpty();
    }
}
