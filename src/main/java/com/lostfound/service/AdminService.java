
package com.lostfound.service;

import com.lostfound.dto.admin.AdminDashboardResponse;
import com.lostfound.entity.FoundItem;
import com.lostfound.entity.LostItem;
import com.lostfound.entity.Match;
import com.lostfound.entity.User;
import com.lostfound.repository.FoundItemRepository;
import com.lostfound.repository.LostItemRepository;
import com.lostfound.repository.MatchRepository;
import com.lostfound.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final LostItemRepository lostItemRepository;
    private final FoundItemRepository foundItemRepository;
    private final MatchRepository matchRepository;

    public AdminService(
            UserRepository userRepository,
            LostItemRepository lostItemRepository,
            FoundItemRepository foundItemRepository,
            MatchRepository matchRepository) {
        this.userRepository = userRepository;
        this.lostItemRepository = lostItemRepository;
        this.foundItemRepository = foundItemRepository;
        this.matchRepository = matchRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        AdminDashboardResponse response = new AdminDashboardResponse();

        List<User> users = userRepository.findAll();
        List<Match> matches = matchRepository.findAll();

        response.setTotalUsers(users.size());
        response.setEnabledUsers(
                users.stream().filter(User::isEnabled).count());
        response.setTotalLostItems(lostItemRepository.count());
        response.setTotalFoundItems(foundItemRepository.count());
        response.setTotalMatches(matches.size());

        response.setPendingMatches(matches.stream()
                .filter(m -> "PENDING".equalsIgnoreCase(m.getStatus()))
                .count());

        response.setConfirmedMatches(matches.stream()
                .filter(m -> "CONFIRMED".equalsIgnoreCase(m.getStatus()))
                .count());

        return response;
    }

    @Transactional(readOnly = true)
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User setUserEnabled(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (user.getRole() == User.Role.ADMIN && !enabled) {
            throw new IllegalArgumentException(
                    "Admin accounts cannot be disabled here");
        }

        user.setEnabled(enabled);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<LostItem> getAllLostItems() {
        return lostItemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<FoundItem> getAllFoundItems() {
        return foundItemRepository.findAll();
    }

    @Transactional
    public LostItem setLostItemStatus(Long id, String status) {
        String normalized = normalizeStatus(status);

        LostItem item = lostItemRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Lost item not found"));

        item.setStatus(normalized);
        return lostItemRepository.save(item);
    }

    @Transactional
    public FoundItem setFoundItemStatus(Long id, String status) {
        String normalized = normalizeStatus(status);

        FoundItem item = foundItemRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Found item not found"));

        item.setStatus(normalized);
        return foundItemRepository.save(item);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }

        String normalized = status.trim().toUpperCase();

        if (!List.of("ACTIVE", "CLOSED").contains(normalized)) {
            throw new IllegalArgumentException(
                    "Status must be ACTIVE or CLOSED");
        }

        return normalized;
    }
}
