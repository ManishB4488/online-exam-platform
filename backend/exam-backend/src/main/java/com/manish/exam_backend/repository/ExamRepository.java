package com.manish.exam_backend.repository;

import com.manish.exam_backend.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, Long> {
}