package com.manish.exam_backend.service;

import com.manish.exam_backend.dto.AnswerItem;
import com.manish.exam_backend.dto.ExamStartResponse;
import com.manish.exam_backend.dto.SubmitRequest;
import com.manish.exam_backend.dto.SubmitResponse;
import com.manish.exam_backend.model.Exam;
import com.manish.exam_backend.model.ExamAttempt;
import com.manish.exam_backend.model.Question;
import com.manish.exam_backend.model.Result;
import com.manish.exam_backend.model.Role;
import com.manish.exam_backend.model.User;
import com.manish.exam_backend.repository.ExamAttemptRepository;
import com.manish.exam_backend.repository.QuestionRepository;
import com.manish.exam_backend.repository.ResultRepository;
import com.manish.exam_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentExamServiceTest {

    private static final String EMAIL = "student@exam.com";

    @Mock ExamService examService;
    @Mock QuestionRepository questionRepo;
    @Mock ResultRepository resultRepo;
    @Mock UserRepository userRepo;
    @Mock ExamAttemptRepository attemptRepo;
    @InjectMocks StudentExamService service;

    private User user;
    private Exam exam;

    @BeforeEach
    void setUp() {
        user = new User(1L, "Student", EMAIL, "hash", Role.STUDENT);
        exam = new Exam();
        exam.setId(1L);
        exam.setTitle("Java Basics");
        exam.setDurationMinutes(10); // 600 seconds

        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(examService.findExam(1L)).thenReturn(exam);
        when(attemptRepo.save(any(ExamAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resultRepo.save(any(Result.class))).thenAnswer(inv -> {
            Result r = inv.getArgument(0);
            r.setId(99L);
            return r;
        });
    }

    // ---------- helpers ----------

    private Question question(long id, String correct, int marks) {
        Question q = new Question();
        q.setId(id);
        q.setText("Question " + id);
        q.setOptionA("a");
        q.setOptionB("b");
        q.setOptionC("c");
        q.setOptionD("d");
        q.setCorrectOption(correct);
        q.setMarks(marks);
        q.setExam(exam);
        return q;
    }

    private void givenQuestions(Question... questions) {
        when(questionRepo.findByExamId(1L)).thenReturn(List.of(questions));
    }

    private ExamAttempt givenAttemptStarted(long secondsAgo) {
        ExamAttempt attempt = new ExamAttempt();
        attempt.setUser(user);
        attempt.setExam(exam);
        attempt.setStartedAt(LocalDateTime.now().minusSeconds(secondsAgo));
        when(attemptRepo.findFirstByUserIdAndExamIdAndSubmittedFalseOrderByStartedAtDesc(1L, 1L))
                .thenReturn(Optional.of(attempt));
        return attempt;
    }

    private SubmitRequest answers(AnswerItem... items) {
        return new SubmitRequest(List.of(items));
    }

    private void assertStatus(Throwable thrown, HttpStatus expected) {
        assertThat(thrown).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(expected));
    }

    // ---------- scoring ----------

    @Test
    void allCorrectAnswersGiveFullMarks() {
        givenQuestions(question(1, "A", 1), question(2, "C", 2));
        givenAttemptStarted(10);

        SubmitResponse res = service.submit(EMAIL, 1L,
                answers(new AnswerItem(1L, "A"), new AnswerItem(2L, "C")));

        assertThat(res.score()).isEqualTo(3);
        assertThat(res.totalMarks()).isEqualTo(3);
        assertThat(res.correctCount()).isEqualTo(2);
        assertThat(res.resultId()).isEqualTo(99L);
    }

    @Test
    void wrongAnswersGiveZero() {
        givenQuestions(question(1, "A", 1), question(2, "C", 2));
        givenAttemptStarted(10);

        SubmitResponse res = service.submit(EMAIL, 1L,
                answers(new AnswerItem(1L, "B"), new AnswerItem(2L, "D")));

        assertThat(res.score()).isEqualTo(0);
        assertThat(res.correctCount()).isEqualTo(0);
    }

    @Test
    void unansweredQuestionsGiveNoMarks() {
        givenQuestions(question(1, "A", 1), question(2, "C", 2));
        givenAttemptStarted(10);

        SubmitResponse res = service.submit(EMAIL, 1L, answers(new AnswerItem(1L, "A")));

        assertThat(res.score()).isEqualTo(1);
        assertThat(res.totalMarks()).isEqualTo(3);
        assertThat(res.review().get(1).selectedOption()).isNull();
        assertThat(res.review().get(1).correct()).isFalse();
    }

    @Test
    void questionIdsFromAnotherExamAreIgnored() {
        givenQuestions(question(1, "A", 1), question(2, "C", 1));
        givenAttemptStarted(10);

        SubmitResponse res = service.submit(EMAIL, 1L,
                answers(new AnswerItem(1L, "A"), new AnswerItem(999L, "A")));

        assertThat(res.score()).isEqualTo(1);
        assertThat(res.review()).hasSize(2);
    }

    @Test
    void submitClosesTheAttemptAndSavesTheResult() {
        givenQuestions(question(1, "A", 1));
        ExamAttempt attempt = givenAttemptStarted(10);

        service.submit(EMAIL, 1L, answers(new AnswerItem(1L, "A")));

        assertThat(attempt.isSubmitted()).isTrue();
        verify(resultRepo).save(any(Result.class));
    }

    // ---------- timer rules ----------

    @Test
    void submitWithoutStartingIsRejected() {
        givenQuestions(question(1, "A", 1));

        Throwable thrown = catchThrowable(
                () -> service.submit(EMAIL, 1L, answers(new AnswerItem(1L, "A"))));

        assertStatus(thrown, HttpStatus.BAD_REQUEST);
        verify(resultRepo, never()).save(any(Result.class));
    }

    @Test
    void submitAfterTheTimeIsOverIsRejected() {
        givenQuestions(question(1, "A", 1));
        givenAttemptStarted(700); // limit 600 seconds + 30 seconds grace = 630

        Throwable thrown = catchThrowable(
                () -> service.submit(EMAIL, 1L, answers(new AnswerItem(1L, "A"))));

        assertStatus(thrown, HttpStatus.GONE);
        verify(resultRepo, never()).save(any(Result.class));
    }

    @Test
    void submitJustAfterTheTimeIsAcceptedBecauseOfTheGracePeriod() {
        givenQuestions(question(1, "A", 1));
        givenAttemptStarted(620); // 20 seconds late, inside the 30 seconds grace

        SubmitResponse res = service.submit(EMAIL, 1L, answers(new AnswerItem(1L, "A")));

        assertThat(res.score()).isEqualTo(1);
    }

    // ---------- starting an exam ----------

    @Test
    void startCreatesANewAttemptWithFullTime() {
        givenQuestions(question(1, "A", 1), question(2, "C", 1));

        ExamStartResponse res = service.start(EMAIL, 1L);

        assertThat(res.secondsLeft()).isBetween(595L, 600L);
        assertThat(res.questions()).hasSize(2);
        verify(attemptRepo).save(any(ExamAttempt.class));
    }

    @Test
    void startContinuesARunningAttemptInsteadOfResettingTheClock() {
        givenQuestions(question(1, "A", 1));
        givenAttemptStarted(100);

        ExamStartResponse res = service.start(EMAIL, 1L);

        assertThat(res.secondsLeft()).isBetween(495L, 500L);
        verify(attemptRepo, never()).save(any(ExamAttempt.class));
    }

    @Test
    void startClosesAnExpiredAttemptAndBeginsANewOne() {
        givenQuestions(question(1, "A", 1));
        ExamAttempt old = givenAttemptStarted(700);

        ExamStartResponse res = service.start(EMAIL, 1L);

        assertThat(old.isSubmitted()).isTrue();
        assertThat(res.secondsLeft()).isBetween(595L, 600L);
        verify(attemptRepo, times(2)).save(any(ExamAttempt.class));
    }

    @Test
    void startRejectsAnExamWithoutQuestions() {
        givenQuestions();

        Throwable thrown = catchThrowable(() -> service.start(EMAIL, 1L));

        assertStatus(thrown, HttpStatus.BAD_REQUEST);
    }
}