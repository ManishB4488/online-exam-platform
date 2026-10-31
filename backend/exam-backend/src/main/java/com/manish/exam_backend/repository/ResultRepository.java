package com.manish.exam_backend.repository;

import com.manish.exam_backend.model.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResultRepository extends JpaRepository<Result, Long> {
    List<Result> findByUserId(Long userId);
    List<Result> findByExamId(Long examId);
    void deleteByExamId(Long examId);
}