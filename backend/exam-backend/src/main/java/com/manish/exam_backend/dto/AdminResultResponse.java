package com.manish.exam_backend.dto;

import java.time.LocalDateTime;

public record AdminResultResponse(Long id, String studentName, String studentEmail,
                                  String examTitle, int score, int totalMarks,
                                  LocalDateTime submittedAt) {}
