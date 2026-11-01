package com.manish.exam_backend.dto;

import java.util.List;

public record SubmitResponse(Long resultId, String examTitle, int score, int totalMarks,
                             int correctCount, int totalQuestions,
                             List<QuestionReview> review) {}