package com.manish.exam_backend.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.manish.exam_backend.dto.AnswerItem;
import com.manish.exam_backend.dto.ExamResponse;
import com.manish.exam_backend.dto.ExamStartResponse;
import com.manish.exam_backend.dto.QuestionReview;
import com.manish.exam_backend.dto.ResultResponse;
import com.manish.exam_backend.dto.StudentQuestionResponse;
import com.manish.exam_backend.dto.SubmitRequest;
import com.manish.exam_backend.dto.SubmitResponse;
import com.manish.exam_backend.model.Exam;
import com.manish.exam_backend.model.ExamAttempt;
import com.manish.exam_backend.model.Question;
import com.manish.exam_backend.model.Result;
import com.manish.exam_backend.model.User;
import com.manish.exam_backend.repository.ExamAttemptRepository;
import com.manish.exam_backend.repository.QuestionRepository;
import com.manish.exam_backend.repository.ResultRepository;
import com.manish.exam_backend.repository.UserRepository;

@Service
public class StudentExamService {

    // Extra seconds allowed after the time is over (network delay, auto-submit)
    private static final long GRACE_SECONDS = 30;

    private final ExamService examService;
    private final QuestionRepository questionRepo;
    private final ResultRepository resultRepo;
    private final UserRepository userRepo;
    private final ExamAttemptRepository attemptRepo;

    public StudentExamService(ExamService examService, QuestionRepository questionRepo,
                              ResultRepository resultRepo, UserRepository userRepo,
                              ExamAttemptRepository attemptRepo) {
        this.examService = examService;
        this.questionRepo = questionRepo;
        this.resultRepo = resultRepo;
        this.userRepo = userRepo;
        this.attemptRepo = attemptRepo;
    }

    // Only exams that already have questions are shown to students
    public List<ExamResponse> listExams() {
        return examService.listAll().stream()
                .filter(e -> e.questionCount() > 0)
                .toList();
    }

    // Starts the exam (or continues a running one) and gives the questions
    // WITHOUT the correct answers. The server keeps the start time.
    @Transactional
    public ExamStartResponse start(String email, Long examId) {
        User user = findUser(email);
        Exam exam = examService.findExam(examId);
        List<Question> questions = questionRepo.findByExamId(examId);
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This exam has no questions yet");
        }

        long limitSeconds = exam.getDurationMinutes() * 60L;
        ExamAttempt attempt = attemptRepo
                .findFirstByUserIdAndExamIdAndSubmittedFalseOrderByStartedAtDesc(user.getId(), examId)
                .orElse(null);

        // An old attempt whose time is over is closed, and a new one begins
        if (attempt != null && secondsLeft(attempt, limitSeconds) <= 0) {
            attempt.setSubmitted(true);
            attemptRepo.save(attempt);
            attempt = null;
        }

        if (attempt == null) {
            attempt = new ExamAttempt();
            attempt.setUser(user);
            attempt.setExam(exam);
            attempt = attemptRepo.save(attempt);
        }

        List<StudentQuestionResponse> list = questions.stream()
                .map(q -> new StudentQuestionResponse(q.getId(), q.getText(), q.getOptionA(),
                        q.getOptionB(), q.getOptionC(), q.getOptionD(), q.getMarks()))
                .toList();
        return new ExamStartResponse(exam.getId(), exam.getTitle(), exam.getDescription(),
                exam.getDurationMinutes(), secondsLeft(attempt, limitSeconds), list);
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

        ExamAttempt attempt = attemptRepo
                .findFirstByUserIdAndExamIdAndSubmittedFalseOrderByStartedAtDesc(user.getId(), examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Start the exam before submitting"));

        long allowedSeconds = exam.getDurationMinutes() * 60L + GRACE_SECONDS;
        if (secondsUsed(attempt) > allowedSeconds) {
            throw new ResponseStatusException(HttpStatus.GONE, "Time is over for this exam");
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

        attempt.setSubmitted(true);
        attemptRepo.save(attempt);

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

    private long secondsUsed(ExamAttempt attempt) {
        return Duration.between(attempt.getStartedAt(), LocalDateTime.now()).getSeconds();
    }

    private long secondsLeft(ExamAttempt attempt, long limitSeconds) {
        return Math.max(0, limitSeconds - secondsUsed(attempt));
    }

    private User findUser(String email) {
        return userRepo.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}