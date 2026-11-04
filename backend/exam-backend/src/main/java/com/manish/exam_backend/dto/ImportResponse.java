package com.manish.exam_backend.dto;

import java.util.List;

public record ImportResponse(Long examId, String title, int questionCount, List<String> warnings) {}
