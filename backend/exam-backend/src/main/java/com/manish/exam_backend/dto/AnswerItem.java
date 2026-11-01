package com.manish.exam_backend.dto;

import jakarta.validation.constraints.*;

public record AnswerItem(
        @NotNull Long questionId,
        @Pattern(regexp = "[ABCD]", message = "selectedOption must be A, B, C or D") String selectedOption
) {}