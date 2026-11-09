package com.manish.exam_backend.config;

import com.manish.exam_backend.model.Role;
import com.manish.exam_backend.model.User;
import com.manish.exam_backend.repository.ExamRepository;
import com.manish.exam_backend.repository.QuestionRepository;
import com.manish.exam_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataLoaderTest {

    @Mock UserRepository userRepo;
    @Mock ExamRepository examRepo;
    @Mock QuestionRepository questionRepo;
    @Mock PasswordEncoder passwordEncoder;

    private DataLoader loader(boolean seedDemoData, String email, String password) {
        return new DataLoader(userRepo, examRepo, questionRepo, passwordEncoder,
                seedDemoData, email, password);
    }

    @Test
    void productionModeCreatesOnlyTheAdminFromTheSecret() {
        when(userRepo.existsByEmail("boss@example.com")).thenReturn(false);
        when(passwordEncoder.encode("a-long-secret-password")).thenReturn("HASHED");

        loader(false, "boss@example.com", "a-long-secret-password").run();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("boss@example.com");
        assertThat(saved.getValue().getPassword()).isEqualTo("HASHED");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
        verify(examRepo, never()).save(any());
        verify(questionRepo, never()).save(any());
    }

    @Test
    void productionModeDoesNotTouchAnExistingAdmin() {
        when(userRepo.existsByEmail("boss@example.com")).thenReturn(true);

        loader(false, "boss@example.com", "a-long-secret-password").run();

        verify(userRepo, never()).save(any());
    }

    @Test
    void productionModeRefusesAShortAdminPassword() {
        DataLoader loader = loader(false, "boss@example.com", "short");

        assertThatThrownBy(() -> loader.run())
                .isInstanceOf(IllegalStateException.class);
        verify(userRepo, never()).save(any());
    }

    @Test
    void productionModeWithoutAdminPasswordCreatesNoUser() {
        loader(false, "boss@example.com", "").run();

        verify(userRepo, never()).save(any());
    }

    @Test
    void demoModeCreatesTheSampleAccountsAndExam() {
        when(userRepo.count()).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("HASHED");
        when(examRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        loader(true, "admin@exam.com", "").run();

        verify(userRepo, org.mockito.Mockito.times(2)).save(any(User.class));
        verify(examRepo).save(any());
        verify(questionRepo, org.mockito.Mockito.times(2)).save(any());
    }
}
