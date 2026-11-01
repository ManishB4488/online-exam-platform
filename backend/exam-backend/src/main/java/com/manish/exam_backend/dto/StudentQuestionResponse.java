package com.manish.exam_backend.dto;

public record StudentQuestionResponse(Long id, String text, String optionA, String optionB,
                                      String optionC, String optionD, int marks) {}