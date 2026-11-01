package com.manish.exam_backend.dto;

public record QuestionReview(Long questionId, String selectedOption,
                             String correctOption, boolean correct) {}