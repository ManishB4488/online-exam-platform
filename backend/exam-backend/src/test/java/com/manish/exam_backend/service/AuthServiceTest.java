package com.manish.exam_backend.service;

import com.manish.exam_backend.dto.AuthResponse;
import com.manish.exam_backend.dto.LoginRequest;
import com.manish.exam_backend.dto.RegisterRequest;
import com.manish.exam_backend.model.Role;
import com.manish.exam_backend.model.User;
import com.manish.exam_backend.repository.UserRepository;
import com.manish.exam_backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    private void assertStatus(Throwable thrown, HttpStatus expected) {
        assertThat(thrown).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(expected));
    }

    @Test
    void registerSavesAStudentWithAHashedPassword() {
        when(userRepo.existsByEmail("new@exam.com")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("HASHED");
        when(jwtService.generateToken(any(User.class))).thenReturn("token123");

        AuthResponse res = authService.register(new RegisterRequest("New", "new@exam.com", "secret1"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.STUDENT);
        assertThat(saved.getValue().getPassword()).isEqualTo("HASHED");
        assertThat(res.token()).isEqualTo("token123");
        assertThat(res.role()).isEqualTo("STUDENT");
    }

    @Test
    void registerWithAUsedEmailGivesConflict() {
        when(userRepo.existsByEmail("used@exam.com")).thenReturn(true);

        Throwable thrown = catchThrowable(
                () -> authService.register(new RegisterRequest("Used", "used@exam.com", "secret1")));

        assertStatus(thrown, HttpStatus.CONFLICT);
    }

    @Test
    void loginWithTheRightPasswordGivesAToken() {
        User user = new User(1L, "Student", "s@exam.com", "HASHED", Role.STUDENT);
        when(userRepo.findByEmail("s@exam.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret1", "HASHED")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("token123");

        AuthResponse res = authService.login(new LoginRequest("s@exam.com", "secret1"));

        assertThat(res.token()).isEqualTo("token123");
        assertThat(res.email()).isEqualTo("s@exam.com");
    }

    @Test
    void loginWithAWrongPasswordGivesUnauthorized() {
        User user = new User(1L, "Student", "s@exam.com", "HASHED", Role.STUDENT);
        when(userRepo.findByEmail("s@exam.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "HASHED")).thenReturn(false);

        Throwable thrown = catchThrowable(
                () -> authService.login(new LoginRequest("s@exam.com", "bad")));

        assertStatus(thrown, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginWithAnUnknownEmailGivesUnauthorized() {
        when(userRepo.findByEmail("nobody@exam.com")).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(
                () -> authService.login(new LoginRequest("nobody@exam.com", "secret1")));

        assertStatus(thrown, HttpStatus.UNAUTHORIZED);
    }
}