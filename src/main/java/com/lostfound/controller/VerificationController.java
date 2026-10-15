
package com.lostfound.controller;

import com.lostfound.dto.verification.VerificationRequestDto;
import com.lostfound.dto.verification.VerificationResponse;
import com.lostfound.service.VerificationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/verifications")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(
            VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping
    public ResponseEntity<VerificationResponse> submitRequest(
            Authentication authentication,
            @Valid @RequestBody VerificationRequestDto request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(verificationService.submitRequest(
                        authentication.getName(), request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<VerificationResponse>> getMyRequests(
            Authentication authentication) {

        return ResponseEntity.ok(
                verificationService.getMyRequests(
                        authentication.getName()));
    }
}
