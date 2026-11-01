package com.manish.exam_backend.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.manish.exam_backend.dto.ExamResponse;
import com.manish.exam_backend.dto.ExamStartResponse;
import com.manish.exam_backend.dto.ResultResponse;
import com.manish.exam_backend.dto.SubmitRequest;
import com.manish.exam_backend.dto.SubmitResponse;
import com.manish.exam_backend.service.StudentExamService;

import jakarta.validation.Valid;

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
    public ExamStartResponse startExam(@PathVariable Long id, Authentication auth) {
        return studentService.start(auth.getName(), id);
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