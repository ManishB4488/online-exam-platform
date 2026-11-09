package com.manish.exam_backend.config;

import com.manish.exam_backend.model.*;
import com.manish.exam_backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    static final int MIN_ADMIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepo;
    private final ExamRepository examRepo;
    private final QuestionRepository questionRepo;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedDemoData;
    private final String adminEmail;
    private final String adminPassword;

    public DataLoader(UserRepository userRepo, ExamRepository examRepo,
                      QuestionRepository questionRepo, PasswordEncoder passwordEncoder,
                      // true on your computer: creates the sample accounts and exam.
                      // The production deployment sets APP_SEED_DEMO_DATA=false.
                      @Value("${app.seed-demo-data:true}") boolean seedDemoData,
                      // used only when the demo data is switched off
                      @Value("${app.admin.email:admin@exam.com}") String adminEmail,
                      @Value("${app.admin.password:}") String adminPassword) {
        this.userRepo = userRepo;
        this.examRepo = examRepo;
        this.questionRepo = questionRepo;
        this.passwordEncoder = passwordEncoder;
        this.seedDemoData = seedDemoData;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (seedDemoData) {
            loadDemoData();
        } else {
            createAdminFromSettings();
        }

        System.out.println("=== Data check: users=" + userRepo.count()
                + ", exams=" + examRepo.count()
                + ", questions=" + questionRepo.count() + " ===");
    }

    // Production: one admin account whose password comes from a secret, nothing else.
    // It is safe to run on every start: an existing admin is never changed.
    void createAdminFromSettings() {
        if (adminPassword == null || adminPassword.isBlank()) {
            System.out.println("=== No demo data and no APP_ADMIN_PASSWORD: no admin account was created ===");
            return;
        }
        if (adminPassword.length() < MIN_ADMIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("APP_ADMIN_PASSWORD must have at least "
                    + MIN_ADMIN_PASSWORD_LENGTH + " characters");
        }
        if (userRepo.existsByEmail(adminEmail)) {
            return;
        }
        userRepo.save(new User(null, "Administrator", adminEmail,
                passwordEncoder.encode(adminPassword), Role.ADMIN));
        System.out.println("=== Admin account created for " + adminEmail + " ===");
    }

    void loadDemoData() {
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
    }
}
