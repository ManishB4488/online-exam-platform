package com.manish.exam_backend.controller;

import com.manish.exam_backend.dto.AdminResultResponse;
import com.manish.exam_backend.service.AdminResultService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminResultController {

    private final AdminResultService resultService;

    public AdminResultController(AdminResultService resultService) {
        this.resultService = resultService;
    }

    @GetMapping("/results")
    public List<AdminResultResponse> allResults() {
        return resultService.listAll();
    }
}
