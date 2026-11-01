package com.manish.exam_backend.service;

import com.manish.exam_backend.dto.AdminResultResponse;
import com.manish.exam_backend.model.Result;
import com.manish.exam_backend.repository.ResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class AdminResultService {

    private final ResultRepository resultRepo;

    public AdminResultService(ResultRepository resultRepo) {
        this.resultRepo = resultRepo;
    }

    // All results of all students, newest first
    @Transactional(readOnly = true)
    public List<AdminResultResponse> listAll() {
        return resultRepo.findAll().stream()
                .sorted(Comparator.comparing(Result::getSubmittedAt).reversed())
                .map(r -> new AdminResultResponse(r.getId(),
                        r.getUser().getName(), r.getUser().getEmail(),
                        r.getExam().getTitle(), r.getScore(), r.getTotalMarks(),
                        r.getSubmittedAt()))
                .toList();
    }
}
