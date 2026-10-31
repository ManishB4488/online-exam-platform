package com.manish.exam_backend.dto;

import jakarta.validation.constraints.*;

public record ExamRequest(
        @NotBlank String title,
        String description,
        @Min(value = 1, message = "Duration must be at least 1 minute") int durationMinutes
) {}