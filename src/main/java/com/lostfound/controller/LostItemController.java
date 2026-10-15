
package com.lostfound.controller;

import com.lostfound.dto.item.LostItemRequest;
import com.lostfound.dto.item.LostItemResponse;
import com.lostfound.service.LostItemService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lost-items")
public class LostItemController {

    private final LostItemService lostItemService;

    public LostItemController(LostItemService lostItemService) {
        this.lostItemService = lostItemService;
    }

    @PostMapping
    public ResponseEntity<LostItemResponse> createLostItem(
            Authentication authentication,
            @Valid @RequestBody LostItemRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lostItemService.createLostItem(
                        authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<LostItemResponse>> getActiveLostItems() {
        return ResponseEntity.ok(
                lostItemService.getActiveLostItems());
    }

    // EXISTING SEARCH API — PRESERVED
    @GetMapping("/search")
    public ResponseEntity<List<LostItemResponse>> searchLostItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String status) {

        return ResponseEntity.ok(
                lostItemService.searchLostItems(
                        keyword, categoryId, location, status));
    }

    // NEW PAGINATION AND SORTING API
    @GetMapping("/search/paginated")
    public ResponseEntity<Page<LostItemResponse>> searchLostItemsPaginated(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                lostItemService.searchLostItemsPaginated(
                        keyword,
                        categoryId,
                        location,
                        status,
                        page,
                        size,
                        sortBy,
                        direction));
    }

    @GetMapping("/my")
    public ResponseEntity<List<LostItemResponse>> getMyLostItems(
            Authentication authentication) {

        return ResponseEntity.ok(
                lostItemService.getMyLostItems(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LostItemResponse> getLostItem(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                lostItemService.getLostItemById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LostItemResponse> updateLostItem(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody LostItemRequest request) {

        return ResponseEntity.ok(
                lostItemService.updateLostItem(
                        authentication.getName(), id, request));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<Map<String, String>> closeLostItem(
            Authentication authentication,
            @PathVariable Long id) {

        lostItemService.closeLostItem(authentication.getName(), id);

        return ResponseEntity.ok(
                Map.of("message", "Lost item closed successfully"));
    }
}
