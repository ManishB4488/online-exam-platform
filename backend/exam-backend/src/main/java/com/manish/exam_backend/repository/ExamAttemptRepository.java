package com.manish.exam_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.manish.exam_backend.model.ExamAttempt;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    Optional<ExamAttempt> findFirstByUserIdAndExamIdAndSubmittedFalseOrderByStartedAtDesc(
            Long userId, Long examId);

    void deleteByExamId(Long examId);
}