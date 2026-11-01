package com.manish.exam_backend.dto;

import java.util.List;

public record ExamStartResponse(Long id, String title, String description,
                                int durationMinutes, List<StudentQuestionResponse> questions) {}