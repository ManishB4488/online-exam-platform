package com.manish.exam_backend.controller;

import com.manish.exam_backend.dto.ImportResponse;
import com.manish.exam_backend.service.ExamImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
public class AdminImportController {

    private final ExamImportService importService;

    public AdminImportController(ExamImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/exams/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportResponse importExam(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("durationMinutes") int durationMinutes,
            @RequestParam(value = "marksPerQuestion", defaultValue = "1") int marksPerQuestion) {
        return importService.importFromPdf(file, title, description, durationMinutes, marksPerQuestion);
    }
}
