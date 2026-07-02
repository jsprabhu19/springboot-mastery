package com.quickeats.userservice.controller;

import com.quickeats.userservice.dto.AuthResponse;
import com.quickeats.userservice.dto.LoginRequest;
import com.quickeats.userservice.dto.RegisterRequest;
import com.quickeats.userservice.dto.UserResponse;
import com.quickeats.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller exposing public endpoints for user registration, authentication, and service testing.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @GetMapping("/test")
    public Map<String, String> test() {
        return Map.of("message", "User Service is up and reachable!");
    }
}
