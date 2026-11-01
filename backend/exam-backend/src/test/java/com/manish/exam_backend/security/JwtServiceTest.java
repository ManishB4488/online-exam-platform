package com.manish.exam_backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.manish.exam_backend.model.Role;
import com.manish.exam_backend.model.User;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-with-more-than-32-characters";

    private final User user = new User(1L, "Student", "s@exam.com", "hash", Role.STUDENT);

    @Test
    void tokenContainsTheEmailAndIsValid() {
        JwtService jwt = new JwtService(SECRET, 60_000);
        String token = jwt.generateToken(user);

        assertThat(jwt.isValid(token)).isTrue();
        assertThat(jwt.extractEmail(token)).isEqualTo("s@exam.com");
    }

    @Test
    void tokenMadeWithAnotherSecretIsRejected() {
        String token = new JwtService(SECRET, 60_000).generateToken(user);
        JwtService other = new JwtService("a-completely-different-secret-key-123456789", 60_000);

        assertThat(other.isValid(token)).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, -1_000);

        assertThat(jwt.isValid(jwt.generateToken(user))).isFalse();
    }

    @Test
    void randomTextIsRejected() {
        assertThat(new JwtService(SECRET, 60_000).isValid("not-a-token")).isFalse();
    }
}