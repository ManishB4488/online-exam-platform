package com.manish.exam_backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/me")
    public String me(Authentication auth) {
        return "Logged in as " + auth.getName() + " with " + auth.getAuthorities();
    }

    @GetMapping("/api/admin/ping")
    public String adminPing() {
        return "Hello Admin";
    }
}