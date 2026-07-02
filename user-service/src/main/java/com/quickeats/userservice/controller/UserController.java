package com.quickeats.userservice.controller;

import com.quickeats.userservice.dto.UserResponse;
import com.quickeats.userservice.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Controller exposing protected endpoints for retrieving user profile details.
 * Implements role-based access checks.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public UserResponse getProfile(Principal principal) {
        String username = principal.getName();
        return userService.getUserByUsername(username);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUserById(@PathVariable("id") Long id) {
        return userService.getUserById(id);
    }
}
