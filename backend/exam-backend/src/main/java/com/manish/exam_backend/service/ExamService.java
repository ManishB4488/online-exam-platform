package com.manish.exam_backend.service;

import com.manish.exam_backend.dto.ExamRequest;
import com.manish.exam_backend.dto.ExamResponse;
import com.manish.exam_backend.model.Exam;
import com.manish.exam_backend.repository.ExamRepository;
import com.manish.exam_backend.repository.QuestionRepository;
import com.manish.exam_backend.repository.ResultRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ExamService {

    private final ExamRepository examRepo;
    private final QuestionRepository questionRepo;
    private final ResultRepository resultRepo;

    public ExamService(ExamRepository examRepo, QuestionRepository questionRepo,
                       ResultRepository resultRepo) {
        this.examRepo = examRepo;
        this.questionRepo = questionRepo;
        this.resultRepo = resultRepo;
    }

    public List<ExamResponse> listAll() {
        return examRepo.findAll().stream().map(this::toResponse).toList();
    }

    public ExamResponse getById(Long id) {
        return toResponse(findExam(id));
    }

    public ExamResponse create(ExamRequest req) {
        Exam exam = new Exam();
        exam.setTitle(req.title());
        exam.setDescription(req.description());
        exam.setDurationMinutes(req.durationMinutes());
        return toResponse(examRepo.save(exam));
    }

    public ExamResponse update(Long id, ExamRequest req) {
        Exam exam = findExam(id);
        exam.setTitle(req.title());
        exam.setDescription(req.description());
        exam.setDurationMinutes(req.durationMinutes());
        return toResponse(examRepo.save(exam));
    }

    @Transactional
    public void delete(Long id) {
        Exam exam = findExam(id);
        // Delete the things that belong to the exam first
        resultRepo.deleteByExamId(id);
        questionRepo.deleteByExamId(id);
        examRepo.delete(exam);
    }

    public Exam findExam(Long id) {
        return examRepo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
    }

    private ExamResponse toResponse(Exam e) {
        return new ExamResponse(e.getId(), e.getTitle(), e.getDescription(),
                e.getDurationMinutes(), questionRepo.countByExamId(e.getId()));
    }
}