package com.manish.exam_backend.config;

import com.manish.exam_backend.model.*;
import com.manish.exam_backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepo;
    private final ExamRepository examRepo;
    private final QuestionRepository questionRepo;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository userRepo, ExamRepository examRepo,
                      QuestionRepository questionRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.examRepo = examRepo;
        this.questionRepo = questionRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepo.count() == 0) {
            // Passwords are saved as BCrypt hashes, not plain text.
            userRepo.save(new User(null, "Admin", "admin@exam.com",
                    passwordEncoder.encode("admin123"), Role.ADMIN));
            userRepo.save(new User(null, "Student One", "student@exam.com",
                    passwordEncoder.encode("student123"), Role.STUDENT));

            Exam exam = new Exam();
            exam.setTitle("Java Basics Test");
            exam.setDescription("Simple questions on Java");
            exam.setDurationMinutes(10);
            exam = examRepo.save(exam);

            Question q1 = new Question();
            q1.setText("Which keyword is used to create an object in Java?");
            q1.setOptionA("new");
            q1.setOptionB("class");
            q1.setOptionC("this");
            q1.setOptionD("static");
            q1.setCorrectOption("A");
            q1.setExam(exam);
            questionRepo.save(q1);

            Question q2 = new Question();
            q2.setText("Which of these is NOT a primitive data type?");
            q2.setOptionA("int");
            q2.setOptionB("boolean");
            q2.setOptionC("String");
            q2.setOptionD("double");
            q2.setCorrectOption("C");
            q2.setExam(exam);
            questionRepo.save(q2);
        }

        System.out.println("=== Data check: users=" + userRepo.count()
                + ", exams=" + examRepo.count()
                + ", questions=" + questionRepo.count() + " ===");
    }
}