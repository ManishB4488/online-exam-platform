package com.manish.exam_backend.dto;

import java.time.LocalDateTime;

public record ResultResponse(Long id, Long examId, String examTitle,
                             int score, int totalMarks, LocalDateTime submittedAt) {}