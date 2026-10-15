
package com.lostfound.service;

import com.lostfound.dto.verification.VerificationRequestDto;
import com.lostfound.dto.verification.VerificationResponse;
import com.lostfound.entity.Match;
import com.lostfound.entity.User;
import com.lostfound.entity.VerificationRequest;
import com.lostfound.repository.MatchRepository;
import com.lostfound.repository.UserRepository;
import com.lostfound.repository.VerificationRequestRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VerificationService {

    private final VerificationRequestRepository verificationRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    public VerificationService(
            VerificationRequestRepository verificationRepository,
            MatchRepository matchRepository,
            UserRepository userRepository) {
        this.verificationRepository = verificationRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public VerificationResponse submitRequest(
            String email, VerificationRequestDto dto) {

        User requester = getUser(email);

        Match match = matchRepository.findById(dto.getMatchId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Match not found"));

        boolean isLostItemOwner =
                match.getLostItem().getUser().getId().equals(requester.getId());

        boolean isFoundItemOwner =
                match.getFoundItem().getUser().getId().equals(requester.getId());

        if (!isLostItemOwner && !isFoundItemOwner) {
            throw new IllegalArgumentException(
                    "You cannot request verification for this match");
        }

        VerificationRequest request = new VerificationRequest();
        request.setMatch(match);
        request.setRequester(requester);
        request.setProofDescription(dto.getProofDescription().trim());
        request.setStatus("PENDING");

        return toResponse(verificationRepository.save(request));
    }

    public List<VerificationResponse> getMyRequests(String email) {
        User requester = getUser(email);

        return verificationRepository
                .findByRequesterIdOrderByCreatedAtDesc(requester.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<VerificationResponse> getAllRequests() {
        return verificationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VerificationResponse reviewRequest(
            Long requestId, String status, String remarks) {

        VerificationRequest request =
                verificationRepository.findById(requestId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Verification request not found"));

        if (!"APPROVED".equalsIgnoreCase(status)
                && !"REJECTED".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException(
                    "Status must be APPROVED or REJECTED");
        }

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalArgumentException(
                    "This request has already been reviewed");
        }

        request.setStatus(status.toUpperCase());
        request.setAdminRemarks(remarks);
        request.setReviewedAt(java.time.LocalDateTime.now());

        return toResponse(verificationRepository.save(request));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }

    private VerificationResponse toResponse(VerificationRequest request) {
        VerificationResponse response = new VerificationResponse();
        response.setId(request.getId());
        response.setMatchId(request.getMatch().getId());
        response.setRequesterId(request.getRequester().getId());
        response.setRequesterName(request.getRequester().getName());
        response.setProofDescription(request.getProofDescription());
        response.setStatus(request.getStatus());
        response.setAdminRemarks(request.getAdminRemarks());
        response.setCreatedAt(request.getCreatedAt());
        response.setReviewedAt(request.getReviewedAt());
        return response;
    }
}
