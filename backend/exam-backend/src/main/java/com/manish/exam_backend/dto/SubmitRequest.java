package com.manish.exam_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitRequest(@NotNull @Valid List<AnswerItem> answers) {}