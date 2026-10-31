package com.manish.exam_backend.controller;

import com.manish.exam_backend.dto.*;
import com.manish.exam_backend.service.ExamService;
import com.manish.exam_backend.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminExamController {

    private final ExamService examService;
    private final QuestionService questionService;

    public AdminExamController(ExamService examService, QuestionService questionService) {
        this.examService = examService;
        this.questionService = questionService;
    }

    // ---------- Exams ----------

    @GetMapping("/exams")
    public List<ExamResponse> listExams() {
        return examService.listAll();
    }

    @GetMapping("/exams/{id}")
    public ExamResponse getExam(@PathVariable Long id) {
        return examService.getById(id);
    }

    @PostMapping("/exams")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamResponse createExam(@Valid @RequestBody ExamRequest req) {
        return examService.create(req);
    }

    @PutMapping("/exams/{id}")
    public ExamResponse updateExam(@PathVariable Long id, @Valid @RequestBody ExamRequest req) {
        return examService.update(id, req);
    }

    @DeleteMapping("/exams/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExam(@PathVariable Long id) {
        examService.delete(id);
    }

    // ---------- Questions ----------

    @GetMapping("/exams/{examId}/questions")
    public List<QuestionResponse> listQuestions(@PathVariable Long examId) {
        return questionService.listByExam(examId);
    }

    @PostMapping("/exams/{examId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse addQuestion(@PathVariable Long examId,
                                        @Valid @RequestBody QuestionRequest req) {
        return questionService.add(examId, req);
    }

    @PutMapping("/questions/{id}")
    public QuestionResponse updateQuestion(@PathVariable Long id,
                                           @Valid @RequestBody QuestionRequest req) {
        return questionService.update(id, req);
    }

    @DeleteMapping("/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(@PathVariable Long id) {
        questionService.delete(id);
    }
}