package com.manish.exam_backend.service;

import com.manish.exam_backend.dto.*;
import com.manish.exam_backend.model.Exam;
import com.manish.exam_backend.model.Question;
import com.manish.exam_backend.model.Result;
import com.manish.exam_backend.model.User;
import com.manish.exam_backend.repository.QuestionRepository;
import com.manish.exam_backend.repository.ResultRepository;
import com.manish.exam_backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class StudentExamService {

    private final ExamService examService;
    private final QuestionRepository questionRepo;
    private final ResultRepository resultRepo;
    private final UserRepository userRepo;

    public StudentExamService(ExamService examService, QuestionRepository questionRepo,
                              ResultRepository resultRepo, UserRepository userRepo) {
        this.examService = examService;
        this.questionRepo = questionRepo;
        this.resultRepo = resultRepo;
        this.userRepo = userRepo;
    }

    // Only exams that already have questions are shown to students
    public List<ExamResponse> listExams() {
        return examService.listAll().stream()
                .filter(e -> e.questionCount() > 0)
                .toList();
    }

    // Gives the questions WITHOUT the correct answers
    public ExamStartResponse start(Long examId) {
        Exam exam = examService.findExam(examId);
        List<Question> questions = questionRepo.findByExamId(examId);
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This exam has no questions yet");
        }
        List<StudentQuestionResponse> list = questions.stream()
                .map(q -> new StudentQuestionResponse(q.getId(), q.getText(), q.getOptionA(),
                        q.getOptionB(), q.getOptionC(), q.getOptionD(), q.getMarks()))
                .toList();
        return new ExamStartResponse(exam.getId(), exam.getTitle(), exam.getDescription(),
                exam.getDurationMinutes(), list);
    }

    // The score is counted here on the server, never on the frontend
    @Transactional
    public SubmitResponse submit(String email, Long examId, SubmitRequest req) {
        User user = findUser(email);
        Exam exam = examService.findExam(examId);
        List<Question> questions = questionRepo.findByExamId(examId);
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This exam has no questions yet");
        }

        // questionId -> selected option (if the same question comes twice, the last one wins)
        Map<Long, String> answers = new HashMap<>();
        for (AnswerItem a : req.answers()) {
            answers.put(a.questionId(), a.selectedOption());
        }

        int score = 0;
        int totalMarks = 0;
        int correctCount = 0;
        List<QuestionReview> review = new ArrayList<>();

        // We loop over the exam's own questions, so extra or fake question ids are ignored
        for (Question q : questions) {
            totalMarks += q.getMarks();
            String selected = answers.get(q.getId());
            boolean correct = selected != null && selected.equals(q.getCorrectOption());
            if (correct) {
                score += q.getMarks();
                correctCount++;
            }
            review.add(new QuestionReview(q.getId(), selected, q.getCorrectOption(), correct));
        }

        Result result = new Result();
        result.setUser(user);
        result.setExam(exam);
        result.setScore(score);
        result.setTotalMarks(totalMarks);
        result = resultRepo.save(result);

        return new SubmitResponse(result.getId(), exam.getTitle(), score, totalMarks,
                correctCount, questions.size(), review);
    }

    @Transactional(readOnly = true)
    public List<ResultResponse> myResults(String email) {
        User user = findUser(email);
        return resultRepo.findByUserId(user.getId()).stream()
                .sorted(Comparator.comparing(Result::getSubmittedAt).reversed())
                .map(r -> new ResultResponse(r.getId(), r.getExam().getId(),
                        r.getExam().getTitle(), r.getScore(), r.getTotalMarks(),
                        r.getSubmittedAt()))
                .toList();
    }

    private User findUser(String email) {
        return userRepo.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}