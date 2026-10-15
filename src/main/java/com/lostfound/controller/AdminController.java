
package com.lostfound.controller;

import com.lostfound.dto.admin.AdminDashboardResponse;
import com.lostfound.dto.verification.VerificationResponse;
import com.lostfound.entity.FoundItem;
import com.lostfound.entity.LostItem;
import com.lostfound.entity.User;
import com.lostfound.service.AdminService;
import com.lostfound.service.VerificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final VerificationService verificationService;
    private final AdminService adminService;

    public AdminController(
            VerificationService verificationService,
            AdminService adminService) {
        this.verificationService = verificationService;
        this.adminService = adminService;
    }

    @GetMapping("/verifications")
    public ResponseEntity<List<VerificationResponse>> getAllVerifications() {
        return ResponseEntity.ok(verificationService.getAllRequests());
    }

    @PatchMapping("/verifications/{id}/review")
    public ResponseEntity<VerificationResponse> reviewVerification(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(
                verificationService.reviewRequest(id, status, remarks));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> adminHealth() {
        return ResponseEntity.ok(
                Map.of("status", "UP", "access", "ADMIN"));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> dashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers() {
        List<Map<String, Object>> result = adminService.getUsers().stream()
                .map(user -> Map.<String, Object>of(
                        "id", user.getId(),
                        "name", user.getName(),
                        "email", user.getEmail(),
                        "role", user.getRole().name(),
                        "enabled", user.isEnabled(),
                        "createdAt", user.getCreatedAt() == null
                                ? "" : user.getCreatedAt().toString()))
                .toList();

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/users/{id}/enabled")
    public ResponseEntity<Map<String, Object>> setUserEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        User user = adminService.setUserEnabled(id, enabled);

        return ResponseEntity.ok(Map.of(
                "message", "User status updated",
                "userId", user.getId(),
                "enabled", user.isEnabled()));
    }

    @GetMapping("/lost-items")
    public ResponseEntity<List<Map<String, Object>>> getLostItems() {
        List<Map<String, Object>> result =
                adminService.getAllLostItems().stream()
                        .map(item -> Map.<String, Object>of(
                                "id", item.getId(),
                                "title", item.getTitle(),
                                "location", item.getLocation() == null
                                        ? "" : item.getLocation(),
                                "status", item.getStatus(),
                                "ownerId", item.getUser().getId(),
                                "category", item.getCategory() == null
                                        ? "" : item.getCategory().getName()))
                        .toList();

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/lost-items/{id}/status")
    public ResponseEntity<Map<String, Object>> setLostItemStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        LostItem item = adminService.setLostItemStatus(id, status);

        return ResponseEntity.ok(Map.of(
                "message", "Lost item status updated",
                "itemId", item.getId(),
                "status", item.getStatus()));
    }

    @GetMapping("/found-items")
    public ResponseEntity<List<Map<String, Object>>> getFoundItems() {
        List<Map<String, Object>> result =
                adminService.getAllFoundItems().stream()
                        .map(item -> Map.<String, Object>of(
                                "id", item.getId(),
                                "title", item.getTitle(),
                                "location", item.getLocation() == null
                                        ? "" : item.getLocation(),
                                "status", item.getStatus(),
                                "ownerId", item.getUser().getId(),
                                "category", item.getCategory() == null
                                        ? "" : item.getCategory().getName()))
                        .toList();

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/found-items/{id}/status")
    public ResponseEntity<Map<String, Object>> setFoundItemStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        FoundItem item = adminService.setFoundItemStatus(id, status);

        return ResponseEntity.ok(Map.of(
                "message", "Found item status updated",
                "itemId", item.getId(),
                "status", item.getStatus()));
    }
}
