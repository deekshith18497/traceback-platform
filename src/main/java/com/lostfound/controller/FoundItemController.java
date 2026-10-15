
package com.lostfound.controller;

import com.lostfound.dto.item.FoundItemRequest;
import com.lostfound.dto.item.FoundItemResponse;
import com.lostfound.service.FoundItemService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/found-items")
public class FoundItemController {

    private final FoundItemService foundItemService;

    public FoundItemController(FoundItemService foundItemService) {
        this.foundItemService = foundItemService;
    }

    @PostMapping
    public ResponseEntity<FoundItemResponse> createFoundItem(
            Authentication authentication,
            @Valid @RequestBody FoundItemRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(foundItemService.createFoundItem(
                        authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<FoundItemResponse>> getActiveFoundItems() {
        return ResponseEntity.ok(
                foundItemService.getActiveFoundItems());
    }

    @GetMapping("/my")
    public ResponseEntity<List<FoundItemResponse>> getMyFoundItems(
            Authentication authentication) {

        return ResponseEntity.ok(
                foundItemService.getMyFoundItems(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoundItemResponse> getFoundItem(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                foundItemService.getFoundItemById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FoundItemResponse> updateFoundItem(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody FoundItemRequest request) {

        return ResponseEntity.ok(
                foundItemService.updateFoundItem(
                        authentication.getName(), id, request));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<Map<String, String>> closeFoundItem(
            Authentication authentication,
            @PathVariable Long id) {

        foundItemService.closeFoundItem(authentication.getName(), id);

        return ResponseEntity.ok(
                Map.of("message", "Found item closed successfully"));
    }
}
