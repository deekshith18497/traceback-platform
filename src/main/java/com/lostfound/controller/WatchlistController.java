
package com.lostfound.controller;

import com.lostfound.dto.watchlist.WatchlistRequest;
import com.lostfound.dto.watchlist.WatchlistResponse;
import com.lostfound.service.WatchlistService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @PostMapping
    public ResponseEntity<WatchlistResponse> saveItem(
            Authentication authentication,
            @Valid @RequestBody WatchlistRequest request) {

        return ResponseEntity.ok(
                watchlistService.saveItem(
                        authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<WatchlistResponse>> getMyWatchlist(
            Authentication authentication) {

        return ResponseEntity.ok(
                watchlistService.getMyWatchlist(
                        authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> removeItem(
            Authentication authentication,
            @PathVariable Long id) {

        watchlistService.removeItem(authentication.getName(), id);

        return ResponseEntity.ok(
                Map.of("message", "Item removed from watchlist"));
    }
}
