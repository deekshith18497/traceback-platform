
package com.lostfound.service;

import com.lostfound.dto.item.FoundItemRequest;
import com.lostfound.dto.item.FoundItemResponse;
import com.lostfound.entity.Category;
import com.lostfound.entity.FoundItem;
import com.lostfound.entity.User;
import com.lostfound.repository.CategoryRepository;
import com.lostfound.repository.FoundItemRepository;
import com.lostfound.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FoundItemService {

    private final FoundItemRepository foundItemRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final MatchService matchService;
    private final AuditLogService auditLogService;

    public FoundItemService(
            FoundItemRepository foundItemRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            MatchService matchService,
            AuditLogService auditLogService) {

        this.foundItemRepository = foundItemRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.matchService = matchService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public FoundItemResponse createFoundItem(
            String email, FoundItemRequest request) {

        User user = getUser(email);
        Category category = getActiveCategory(request.getCategoryId());

        String title = request.getTitle().trim();
        String description = request.getDescription().trim();
        String location = request.getLocation().trim();

        boolean duplicate = foundItemRepository
                .existsByUserIdAndCategoryIdAndStatusIgnoreCaseAndTitleIgnoreCaseAndLocationIgnoreCase(
                        user.getId(),
                        category.getId(),
                        "ACTIVE",
                        title,
                        location);

        if (duplicate) {
            recordDuplicateAttempt(user.getEmail());

            throw new IllegalArgumentException(
                    "You have already reported this found item. "
                            + "Please check My Found Items before submitting again.");
        }

        FoundItem item = new FoundItem();
        item.setTitle(title);
        item.setDescription(description);
        item.setLocation(location);
        item.setFoundDate(request.getFoundDate());
        item.setLatitude(request.getLatitude());
        item.setLongitude(request.getLongitude());
        item.setImageUrl(request.getImageUrl());
        item.setUser(user);
        item.setCategory(category);
        item.setStatus("ACTIVE");

        FoundItem savedItem = foundItemRepository.save(item);

        // Preserve existing automatic matching.
        matchService.findMatchesForFoundItem(savedItem.getId());

        return toResponse(savedItem);
    }

    private void recordDuplicateAttempt(String email) {
        try {
            auditLogService.recordEvent(
                    "DUPLICATE_REPORT_BLOCKED",
                    email,
                    "Repeated active found-item report rejected",
                    null
            );
        } catch (Exception exception) {
            System.err.println(
                    "Unable to save duplicate-report audit event");
        }
    }

    public List<FoundItemResponse> getActiveFoundItems() {
        return foundItemRepository
                .findByStatusIgnoreCaseOrderByCreatedAtDesc("ACTIVE")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<FoundItemResponse> getMyFoundItems(String email) {
        User user = getUser(email);

        return foundItemRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public FoundItemResponse getFoundItemById(Long id) {
        return toResponse(getFoundItem(id));
    }

    @Transactional
    public FoundItemResponse updateFoundItem(
            String email, Long id, FoundItemRequest request) {

        User user = getUser(email);
        FoundItem item = getFoundItem(id);

        if (!item.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "You can update only your own found items");
        }

        if (!"FOUND".equalsIgnoreCase(item.getStatus())) {
            throw new IllegalArgumentException(
                    "Only active found items can be updated");
        }

        Category category = getActiveCategory(request.getCategoryId());

        item.setTitle(request.getTitle().trim());
        item.setDescription(request.getDescription().trim());
        item.setLocation(request.getLocation().trim());
        item.setFoundDate(request.getFoundDate());
        item.setLatitude(request.getLatitude());
        item.setLongitude(request.getLongitude());
        item.setImageUrl(request.getImageUrl());
        item.setCategory(category);

        return toResponse(foundItemRepository.save(item));
    }

    @Transactional
    public void closeFoundItem(String email, Long id) {
        User user = getUser(email);
        FoundItem item = getFoundItem(id);

        if (!item.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "You can close only your own found items");
        }

        item.setStatus("CLOSED");
        foundItemRepository.save(item);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }

    private Category getActiveCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Category not found"));

        if (!category.isActive()) {
            throw new IllegalArgumentException("Category is inactive");
        }

        return category;
    }

    private FoundItem getFoundItem(Long id) {
        return foundItemRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Found item not found"));
    }

    private FoundItemResponse toResponse(FoundItem item) {
        FoundItemResponse response = new FoundItemResponse();

        response.setId(item.getId());
        response.setTitle(item.getTitle());
        response.setDescription(item.getDescription());
        response.setLocation(item.getLocation());

        response.setLatitude(
                item.getLatitude() == null
                        ? null
                        : Math.round(item.getLatitude() * 1000.0) / 1000.0);

        response.setLongitude(
                item.getLongitude() == null
                        ? null
                        : Math.round(item.getLongitude() * 1000.0) / 1000.0);

        response.setImageUrl(item.getImageUrl());
        response.setFoundDate(item.getFoundDate());
        response.setStatus(item.getStatus());

        response.setUserId(item.getUser().getId());
        response.setUserName(item.getUser().getName());
        response.setUserEmail(item.getUser().getEmail());
        response.setUserPhone(item.getUser().getPhone());

        response.setCategoryId(item.getCategory().getId());
        response.setCategoryName(item.getCategory().getName());
        response.setCreatedAt(item.getCreatedAt());

        return response;
    }
}
