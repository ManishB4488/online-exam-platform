package com.manish.exam_backend.controller;

import com.manish.exam_backend.dto.*;
import com.manish.exam_backend.service.StudentExamService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final StudentExamService studentService;

    public StudentController(StudentExamService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/exams")
    public List<ExamResponse> listExams() {
        return studentService.listExams();
    }

    @GetMapping("/exams/{id}/start")
    public ExamStartResponse startExam(@PathVariable Long id) {
        return studentService.start(id);
    }

    @PostMapping("/exams/{id}/submit")
    public SubmitResponse submit(@PathVariable Long id,
                                 @Valid @RequestBody SubmitRequest req,
                                 Authentication auth) {
        return studentService.submit(auth.getName(), id, req);
    }

    @GetMapping("/results")
    public List<ResultResponse> myResults(Authentication auth) {
        return studentService.myResults(auth.getName());
    }
}