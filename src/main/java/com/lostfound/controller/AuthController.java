package com.lostfound.controller;

import com.lostfound.dto.auth.AuthResponse;
import com.lostfound.dto.auth.LoginRequest;
import com.lostfound.dto.auth.OtpContactRequest;
import com.lostfound.dto.auth.OtpVerificationRequest;
import com.lostfound.dto.auth.PasswordResetRequest;
import com.lostfound.dto.auth.PasswordResetVerificationResponse;
import com.lostfound.dto.auth.RegistrationOtpRequest;
import com.lostfound.service.AuthService;
import com.lostfound.service.OtpAuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpAuthService otpAuthService;

    public AuthController(
            AuthService authService,
            OtpAuthService otpAuthService) {
        this.authService = authService;
        this.otpAuthService = otpAuthService;
    }

    /**
     * Direct registration is disabled to prevent bypassing OTP verification.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerRequiresOtp() {
        return ResponseEntity.status(HttpStatus.GONE).body(Map.of(
                "message",
                "Registration requires OTP verification. Use /auth/register/request-otp and /auth/register/verify-otp."
        ));
    }

    @PostMapping("/register/request-otp")
    public ResponseEntity<Map<String, String>> requestRegistrationOtp(
            @Valid @RequestBody RegistrationOtpRequest request) {

        String message =
                otpAuthService.requestRegistrationOtp(request);

        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<AuthResponse> verifyRegistrationOtp(
            @Valid @RequestBody OtpVerificationRequest request) {

        AuthResponse response =
                otpAuthService.verifyRegistrationOtp(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/password/forgot/request-otp")
    public ResponseEntity<Map<String, String>> requestPasswordResetOtp(
            @Valid @RequestBody OtpContactRequest request) {

        String message =
                otpAuthService.requestPasswordResetOtp(request);

        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/password/forgot/verify-otp")
    public ResponseEntity<PasswordResetVerificationResponse>
            verifyPasswordResetOtp(
                    @Valid @RequestBody OtpVerificationRequest request) {

        return ResponseEntity.ok(
                otpAuthService.verifyPasswordResetOtp(request));
    }

    @PostMapping("/password/forgot/reset")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request) {

        String message =
                otpAuthService.resetPassword(request);

        return ResponseEntity.ok(Map.of("message", message));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "authentication"
        ));
    }
}