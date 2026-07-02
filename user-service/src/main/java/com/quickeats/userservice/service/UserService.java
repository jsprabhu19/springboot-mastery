package com.quickeats.userservice.service;

import com.quickeats.userservice.dto.AuthResponse;
import com.quickeats.userservice.dto.LoginRequest;
import com.quickeats.userservice.dto.RegisterRequest;
import com.quickeats.userservice.dto.UserResponse;

/**
 * Service interface specifying user authentication and profile management business capabilities.
 */
public interface UserService {
    UserResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponse getUserByUsername(String username);
    UserResponse getUserById(Long id);
}
