package com.manish.exam_backend.service;

import com.manish.exam_backend.dto.ImportResponse;
import com.manish.exam_backend.model.Exam;
import com.manish.exam_backend.model.Question;
import com.manish.exam_backend.repository.ExamRepository;
import com.manish.exam_backend.repository.QuestionRepository;
import com.manish.exam_backend.service.PdfQuestionParser.ParseResult;
import com.manish.exam_backend.service.PdfQuestionParser.ParsedQuestion;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExamImportService {

    private static final int MAX_PAGES = 200;
    private static final int MAX_WARNINGS = 20;

    private final ExamRepository examRepo;
    private final QuestionRepository questionRepo;

    public ExamImportService(ExamRepository examRepo, QuestionRepository questionRepo) {
        this.examRepo = examRepo;
        this.questionRepo = questionRepo;
    }

    @Transactional
    public ImportResponse importFromPdf(MultipartFile file, String title, String description,
                                        int durationMinutes, int marksPerQuestion) {
        if (title == null || title.isBlank()) {
            throw bad("Title is required.");
        }
        if (durationMinutes < 1) {
            throw bad("Duration must be at least 1 minute.");
        }
        if (marksPerQuestion < 1) {
            throw bad("Marks per question must be at least 1.");
        }

        String text = extractText(readPdfBytes(file));
        ParseResult parsed = PdfQuestionParser.parse(text);

        if (parsed.questions().isEmpty()) {
            throw bad("No questions were found in this PDF. It must be a text PDF (not a scan) "
                    + "with numbered questions, options A to D and the answers.");
        }

        Exam exam = new Exam();
        exam.setTitle(title.trim());
        exam.setDescription(description == null || description.isBlank() ? null : description.trim());
        exam.setDurationMinutes(durationMinutes);
        exam = examRepo.save(exam);

        List<Question> questions = new ArrayList<>();
        for (ParsedQuestion p : parsed.questions()) {
            Question q = new Question();
            q.setExam(exam);
            q.setText(p.text());
            q.setOptionA(p.optionA());
            q.setOptionB(p.optionB());
            q.setOptionC(p.optionC());
            q.setOptionD(p.optionD());
            q.setCorrectOption(p.correctOption());
            q.setMarks(marksPerQuestion);
            questions.add(q);
        }
        questionRepo.saveAll(questions);

        List<String> warnings = new ArrayList<>(parsed.warnings());
        if (warnings.size() > MAX_WARNINGS) {
            int more = warnings.size() - MAX_WARNINGS;
            warnings = new ArrayList<>(warnings.subList(0, MAX_WARNINGS));
            warnings.add("... and " + more + " more.");
        }
        return new ImportResponse(exam.getId(), exam.getTitle(), questions.size(), warnings);
    }

    private byte[] readPdfBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw bad("Please choose a PDF file.");
        }
        try {
            byte[] bytes = file.getBytes();
            // Every real PDF starts with "%PDF-"
            boolean looksLikePdf = bytes.length > 5
                    && new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-");
            if (!looksLikePdf) {
                throw bad("This file is not a valid PDF.");
            }
            return bytes;
        } catch (IOException e) {
            throw bad("Could not read the uploaded file.");
        }
    }

    private String extractText(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.getNumberOfPages() > MAX_PAGES) {
                throw bad("The PDF has too many pages (the maximum is " + MAX_PAGES + ").");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (IOException e) {
            throw bad("Could not read this PDF. It may be damaged or protected by a password.");
        }
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
