
package com.lostfound.service;

import com.lostfound.dto.match.MatchResponse;
import com.lostfound.entity.FoundItem;
import com.lostfound.entity.LostItem;
import com.lostfound.entity.Match;
import com.lostfound.entity.User;
import com.lostfound.matching.MatchingEngine;
import com.lostfound.repository.FoundItemRepository;
import com.lostfound.repository.LostItemRepository;
import com.lostfound.repository.MatchRepository;
import com.lostfound.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class MatchService {

    private final EmailNotificationService emailNotificationService;
    private final MatchRepository matchRepository;
    private final LostItemRepository lostItemRepository;
    private final FoundItemRepository foundItemRepository;
    private final UserRepository userRepository;
    private final MatchingEngine matchingEngine;

    public MatchService(
            EmailNotificationService emailNotificationService,
            MatchRepository matchRepository,
            LostItemRepository lostItemRepository,
            FoundItemRepository foundItemRepository,
            UserRepository userRepository,
            MatchingEngine matchingEngine) {
        this.emailNotificationService = emailNotificationService;
        this.matchRepository = matchRepository;
        this.lostItemRepository = lostItemRepository;
        this.foundItemRepository = foundItemRepository;
        this.userRepository = userRepository;
        this.matchingEngine = matchingEngine;
    }

    @Transactional
    public void confirmMatch(String identifier, Long matchId) {
        User user = getUser(identifier);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Match not found"));

        LostItem lostItem = match.getLostItem();
        FoundItem foundItem = match.getFoundItem();

        boolean lostOwner =
                lostItem.getUser().getId().equals(user.getId());
        boolean foundOwner =
                foundItem.getUser().getId().equals(user.getId());

        if (!lostOwner && !foundOwner) {
            throw new AccessDeniedException(
                    "You cannot confirm another user's match");
        }

        if ("DISMISSED".equalsIgnoreCase(match.getStatus())) {
            throw new IllegalArgumentException(
                    "A dismissed match cannot be confirmed");
        }

        if ("CONFIRMED".equalsIgnoreCase(match.getStatus())) {
            throw new IllegalArgumentException("Match is already confirmed");
        }

        if (!"ACTIVE".equalsIgnoreCase(lostItem.getStatus())
                || !"ACTIVE".equalsIgnoreCase(foundItem.getStatus())) {
            throw new IllegalArgumentException(
                    "Lost item or found item is no longer available");
        }

        boolean lostAlreadyMatched =
                matchRepository.findByLostItemId(lostItem.getId())
                        .stream()
                        .anyMatch(existing ->
                                !existing.getId().equals(match.getId())
                                && "CONFIRMED".equalsIgnoreCase(
                                        existing.getStatus()));

        if (lostAlreadyMatched) {
            throw new IllegalArgumentException(
                    "This lost item is already matched with another found item");
        }

        boolean foundAlreadyMatched =
                matchRepository.findByFoundItemId(foundItem.getId())
                        .stream()
                        .anyMatch(existing ->
                                !existing.getId().equals(match.getId())
                                && "CONFIRMED".equalsIgnoreCase(
                                        existing.getStatus()));

        if (foundAlreadyMatched) {
            throw new IllegalArgumentException(
                    "This found item is already matched with another lost item");
        }

        match.setStatus("CONFIRMED");
        lostItem.setStatus("RESOLVED");
        foundItem.setStatus("RETURNED");

        matchRepository.save(match);
        lostItemRepository.save(lostItem);
        foundItemRepository.save(foundItem);

        emailNotificationService.notifyMatchConfirmed(match);
    }

    @Transactional
    public List<MatchResponse> findMatchesForFoundItem(Long foundItemId) {
        FoundItem foundItem = foundItemRepository.findById(foundItemId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Found item not found"));

        if (!"ACTIVE".equalsIgnoreCase(foundItem.getStatus())) {
            return List.of();
        }

        List<LostItem> lostItems =
                lostItemRepository
                        .findByStatusIgnoreCaseOrderByCreatedAtDesc("ACTIVE");

        List<MatchResponse> responses = new ArrayList<>();

        for (LostItem lostItem : lostItems) {
            if (lostItem.getUser().getId()
                    .equals(foundItem.getUser().getId())) {
                continue;
            }

            double score = matchingEngine.calculateSimilarity(
                    lostItem, foundItem);

            if (score < 30) {
                continue;
            }

            Match match = matchRepository
                    .findByLostItemIdAndFoundItemId(
                            lostItem.getId(), foundItem.getId())
                    .orElseGet(Match::new);

            boolean isNew = match.getId() == null;

            if (!isNew && (
                    "DISMISSED".equalsIgnoreCase(match.getStatus())
                    || "CONFIRMED".equalsIgnoreCase(match.getStatus()))) {
                continue;
            }

            match.setLostItem(lostItem);
            match.setFoundItem(foundItem);
            match.setSimilarityScore(score);
            match.setMatchReason(
                    matchingEngine.explainMatch(lostItem, foundItem));

            if (isNew) {
                match.setStatus("PENDING");
            }

            Match savedMatch = matchRepository.save(match);

            if (isNew) {
                emailNotificationService.notifyPotentialMatch(
                        lostItem.getUser(), savedMatch);
                emailNotificationService.notifyPotentialMatch(
                        foundItem.getUser(), savedMatch);
            }

            responses.add(toResponse(savedMatch));
        }

        responses.sort((a, b) ->
                Double.compare(
                        b.getSimilarityScore(),
                        a.getSimilarityScore()));

        return responses;
    }

    @Transactional
    public void dismissMatch(String identifier, Long matchId) {
        User user = getUser(identifier);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Match not found"));

        boolean lostOwner =
                match.getLostItem().getUser().getId().equals(user.getId());
        boolean foundOwner =
                match.getFoundItem().getUser().getId().equals(user.getId());

        if (!lostOwner && !foundOwner) {
            throw new AccessDeniedException(
                    "You cannot dismiss another user's match");
        }

        if ("CONFIRMED".equalsIgnoreCase(match.getStatus())) {
            throw new IllegalArgumentException(
                    "A confirmed match cannot be dismissed");
        }

        if ("DISMISSED".equalsIgnoreCase(match.getStatus())) {
            throw new IllegalArgumentException("Match is already dismissed");
        }

        match.setStatus("DISMISSED");
        matchRepository.save(match);
    }

    @Transactional
    public List<MatchResponse> findMatchesForLostItem(
            String identifier, Long lostItemId) {
        User user = getUser(identifier);

        LostItem lostItem = lostItemRepository.findById(lostItemId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Lost item not found"));

        if (!lostItem.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    "You can request matches only for your own lost items");
        }

        if (!"ACTIVE".equalsIgnoreCase(lostItem.getStatus())) {
            throw new IllegalArgumentException(
                    "Only active lost items can be matched");
        }

        List<FoundItem> candidates =
                foundItemRepository
                        .findByStatusIgnoreCaseOrderByCreatedAtDesc("ACTIVE");

        List<MatchResponse> responses = new ArrayList<>();

        for (FoundItem foundItem : candidates) {
            if (foundItem.getUser().getId().equals(user.getId())) {
                continue;
            }

            if (!"ACTIVE".equalsIgnoreCase(foundItem.getStatus())) {
                continue;
            }

            double score = matchingEngine.calculateSimilarity(
                    lostItem, foundItem);

            if (score < 30) {
                continue;
            }

            Match match = matchRepository
                    .findByLostItemIdAndFoundItemId(
                            lostItem.getId(), foundItem.getId())
                    .orElseGet(Match::new);

            boolean isNew = match.getId() == null;

            if (!isNew && (
                    "DISMISSED".equalsIgnoreCase(match.getStatus())
                    || "CONFIRMED".equalsIgnoreCase(match.getStatus()))) {
                continue;
            }

            match.setLostItem(lostItem);
            match.setFoundItem(foundItem);
            match.setSimilarityScore(score);
            match.setMatchReason(
                    matchingEngine.explainMatch(lostItem, foundItem));

            if (isNew) {
                match.setStatus("PENDING");
            }

            Match savedMatch = matchRepository.save(match);

            if (isNew) {
                emailNotificationService.notifyPotentialMatch(
                        lostItem.getUser(), savedMatch);
                emailNotificationService.notifyPotentialMatch(
                        foundItem.getUser(), savedMatch);
            }

            responses.add(toResponse(savedMatch));
        }

        responses.sort((a, b) ->
                Double.compare(
                        b.getSimilarityScore(),
                        a.getSimilarityScore()));

        return responses;
    }

    public List<MatchResponse> getMyMatches(String identifier) {
        User user = getUser(identifier);

        List<Match> matches = new ArrayList<>();
        matches.addAll(
                matchRepository.findByLostItemUserIdOrderByCreatedAtDesc(
                        user.getId()));
        matches.addAll(
                matchRepository.findByFoundItemUserIdOrderByCreatedAtDesc(
                        user.getId()));

        return matches.stream()
                .distinct()
                .map(this::toResponse)
                .toList();
    }

    private User getUser(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("User identifier is required");
        }

        return userRepository.findByLoginIdentifier(identifier.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }

    private MatchResponse toResponse(Match match) {
        MatchResponse response = new MatchResponse();
        response.setId(match.getId());
        response.setLostItemId(match.getLostItem().getId());
        response.setLostItemTitle(match.getLostItem().getTitle());
        response.setFoundItemId(match.getFoundItem().getId());
        response.setFoundItemTitle(match.getFoundItem().getTitle());
        response.setSimilarityScore(match.getSimilarityScore());
        response.setMatchReason(match.getMatchReason());
        response.setStatus(match.getStatus());
        response.setCreatedAt(match.getCreatedAt());
        return response;
    }
}
