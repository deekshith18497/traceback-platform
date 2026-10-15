
package com.lostfound.controller;

import com.lostfound.dto.user.ChangePasswordRequest;
import com.lostfound.dto.user.UpdateProfileRequest;
import com.lostfound.dto.user.UserResponse;
import com.lostfound.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(
            Authentication authentication) {
        return ResponseEntity.ok(
                userService.getProfile(authentication.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {

        return ResponseEntity.ok(
                userService.updateProfile(
                        authentication.getName(), request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(authentication.getName(), request);

        return ResponseEntity.ok(
                Map.of("message", "Password changed successfully"));
    }
}
