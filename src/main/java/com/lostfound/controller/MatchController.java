
package com.lostfound.controller;

import com.lostfound.dto.match.MatchResponse;
import com.lostfound.service.MatchService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping("/lost-item/{lostItemId}/find")
    public ResponseEntity<List<MatchResponse>> findMatches(
            Authentication authentication,
            @PathVariable Long lostItemId) {

        return ResponseEntity.ok(
                matchService.findMatchesForLostItem(
                        authentication.getName(), lostItemId));
    }


@PatchMapping("/{matchId}/confirm")
public ResponseEntity<Map<String, String>> confirmMatch(
        Authentication authentication,
        @PathVariable Long matchId) {

    matchService.confirmMatch(
            authentication.getName(), matchId);

    return ResponseEntity.ok(
            Map.of("message", "Match confirmed successfully"));
}

    @GetMapping("/my")
    public ResponseEntity<List<MatchResponse>> getMyMatches(
            Authentication authentication) {

        return ResponseEntity.ok(
                matchService.getMyMatches(authentication.getName()));
    }

    @PatchMapping("/{matchId}/dismiss")
    public ResponseEntity<Map<String, String>> dismissMatch(
            Authentication authentication,
            @PathVariable Long matchId) {

        matchService.dismissMatch(authentication.getName(), matchId);

        return ResponseEntity.ok(
                Map.of("message", "Match dismissed successfully"));
    }
}
