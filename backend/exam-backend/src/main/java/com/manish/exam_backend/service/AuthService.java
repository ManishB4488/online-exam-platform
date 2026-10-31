package com.manish.exam_backend.service;

import com.manish.exam_backend.dto.AuthResponse;
import com.manish.exam_backend.dto.LoginRequest;
import com.manish.exam_backend.dto.RegisterRequest;
import com.manish.exam_backend.model.Role;
import com.manish.exam_backend.model.User;
import com.manish.exam_backend.repository.UserRepository;
import com.manish.exam_backend.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepo, PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        // Everyone who registers here is a STUDENT. Admins are never created from outside.
        User user = new User(null, req.name(), req.email(),
                passwordEncoder.encode(req.password()), Role.STUDENT);
        userRepo.save(user);
        return toResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user),
                user.getName(), user.getEmail(), user.getRole().name());
    }
}