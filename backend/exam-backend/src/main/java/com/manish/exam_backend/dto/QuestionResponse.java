package com.manish.exam_backend.dto;

public record QuestionResponse(Long id, String text, String optionA, String optionB,
                               String optionC, String optionD,
                               String correctOption, int marks) {}