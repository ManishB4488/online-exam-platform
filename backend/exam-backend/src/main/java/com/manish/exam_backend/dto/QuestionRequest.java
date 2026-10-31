package com.manish.exam_backend.dto;

import jakarta.validation.constraints.*;

public record QuestionRequest(
        @NotBlank String text,
        @NotBlank String optionA,
        @NotBlank String optionB,
        @NotBlank String optionC,
        @NotBlank String optionD,
        @NotBlank @Pattern(regexp = "[ABCD]", message = "correctOption must be A, B, C or D") String correctOption,
        @Min(value = 1, message = "Marks must be at least 1") int marks
) {}