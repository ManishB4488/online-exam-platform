package com.manish.exam_backend.service;

import com.manish.exam_backend.dto.QuestionRequest;
import com.manish.exam_backend.dto.QuestionResponse;
import com.manish.exam_backend.model.Exam;
import com.manish.exam_backend.model.Question;
import com.manish.exam_backend.repository.QuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class QuestionService {

    private final QuestionRepository questionRepo;
    private final ExamService examService;

    public QuestionService(QuestionRepository questionRepo, ExamService examService) {
        this.questionRepo = questionRepo;
        this.examService = examService;
    }

    public List<QuestionResponse> listByExam(Long examId) {
        examService.findExam(examId); // gives 404 if the exam does not exist
        return questionRepo.findByExamId(examId).stream().map(this::toResponse).toList();
    }

    public QuestionResponse add(Long examId, QuestionRequest req) {
        Exam exam = examService.findExam(examId);
        Question q = new Question();
        q.setExam(exam);
        copy(req, q);
        return toResponse(questionRepo.save(q));
    }

    public QuestionResponse update(Long questionId, QuestionRequest req) {
        Question q = findQuestion(questionId);
        copy(req, q);
        return toResponse(questionRepo.save(q));
    }

    public void delete(Long questionId) {
        questionRepo.delete(findQuestion(questionId));
    }

    private Question findQuestion(Long id) {
        return questionRepo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
    }

    private void copy(QuestionRequest req, Question q) {
        q.setText(req.text());
        q.setOptionA(req.optionA());
        q.setOptionB(req.optionB());
        q.setOptionC(req.optionC());
        q.setOptionD(req.optionD());
        q.setCorrectOption(req.correctOption());
        q.setMarks(req.marks());
    }

    private QuestionResponse toResponse(Question q) {
        return new QuestionResponse(q.getId(), q.getText(), q.getOptionA(), q.getOptionB(),
                q.getOptionC(), q.getOptionD(), q.getCorrectOption(), q.getMarks());
    }
}