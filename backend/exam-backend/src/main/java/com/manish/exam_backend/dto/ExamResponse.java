package com.manish.exam_backend.dto;

public record ExamResponse(Long id, String title, String description,
                           int durationMinutes, long questionCount) {}