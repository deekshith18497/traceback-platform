
package com.lostfound.service;

import com.lostfound.dto.item.LostItemRequest;
import com.lostfound.dto.item.LostItemResponse;
import com.lostfound.entity.Category;
import com.lostfound.entity.LostItem;
import com.lostfound.entity.User;
import com.lostfound.repository.CategoryRepository;
import com.lostfound.repository.LostItemRepository;
import com.lostfound.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class LostItemService {

    private final LostItemRepository lostItemRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final MatchService matchService;
    private final AuditLogService auditLogService;

    public LostItemService(
            LostItemRepository lostItemRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            MatchService matchService,
            AuditLogService auditLogService) {

        this.lostItemRepository = lostItemRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.matchService = matchService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public LostItemResponse createLostItem(
            String email, LostItemRequest request) {

        User user = getUser(email);
        Category category = getActiveCategory(request.getCategoryId());

        String title = request.getTitle().trim();
        String description = request.getDescription().trim();
        String location = request.getLocation().trim();

        boolean duplicate =
                lostItemRepository
                        .existsByUserIdAndCategoryIdAndStatusIgnoreCaseAndTitleIgnoreCaseAndLocationIgnoreCase(
                                user.getId(),
                                category.getId(),
                                "ACTIVE",
                                title,
                                location);

        if (duplicate) {
            recordDuplicateAttempt(user.getEmail());

            throw new IllegalArgumentException(
                    "You have already reported this lost item. "
                            + "Please check My Lost Items before submitting again.");
        }

        LostItem item = new LostItem();
        item.setTitle(title);
        item.setDescription(description);
        item.setLocation(location);
        item.setLostDate(request.getLostDate());
        item.setLatitude(request.getLatitude());
        item.setLongitude(request.getLongitude());
        item.setImageUrl(request.getImageUrl());
        item.setUser(user);
        item.setCategory(category);
        item.setStatus("ACTIVE");

        LostItem savedItem = lostItemRepository.save(item);

        // Preserve existing automatic matching.
        matchService.findMatchesForLostItem(email, savedItem.getId());

        return toResponse(savedItem);
    }

    private void recordDuplicateAttempt(String email) {
        try {
            auditLogService.recordEvent(
                    "DUPLICATE_REPORT_BLOCKED",
                    email,
                    "Repeated active lost-item report rejected",
                    null
            );
        } catch (Exception exception) {
            System.err.println(
                    "Unable to save duplicate-report audit event");
        }
    }

    public List<LostItemResponse> getActiveLostItems() {
        return lostItemRepository
                .findByStatusIgnoreCaseOrderByCreatedAtDesc("ACTIVE")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<LostItemResponse> getMyLostItems(String email) {
        User user = getUser(email);

        return lostItemRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<LostItemResponse> searchLostItemsPaginated(
            String keyword,
            Long categoryId,
            String location,
            String status,
            int page,
            int size,
            String sortBy,
            String direction) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100");
        }

        keyword = normalizeFilter(keyword);
        location = normalizeFilter(location);
        status = normalizeFilter(status);

        if (status == null) {
            status = "ACTIVE";
        }

        Set<String> allowedSortFields =
                Set.of("createdAt", "title", "lostDate", "location");

        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }

        if (!allowedSortFields.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sortBy field. Allowed fields: "
                            + allowedSortFields);
        }

        Sort.Direction sortDirection =
                "asc".equalsIgnoreCase(direction)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy));

        return lostItemRepository.searchLostItemsPaginated(
                keyword,
                categoryId,
                location,
                status,
                pageable)
                .map(this::toResponse);
    }

    public LostItemResponse getLostItemById(Long id) {
        return toResponse(getLostItem(id));
    }

    @Transactional
    public LostItemResponse updateLostItem(
            String email, Long id, LostItemRequest request) {

        User user = getUser(email);
        LostItem item = getLostItem(id);

        if (!item.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "You can update only your own lost items");
        }

        if (!"ACTIVE".equalsIgnoreCase(item.getStatus())) {
            throw new IllegalArgumentException(
                    "Only active lost items can be updated");
        }

        Category category = getActiveCategory(request.getCategoryId());

        item.setTitle(request.getTitle().trim());
        item.setDescription(request.getDescription().trim());
        item.setLocation(request.getLocation().trim());
        item.setLostDate(request.getLostDate());
        item.setLatitude(request.getLatitude());
        item.setLongitude(request.getLongitude());
        item.setImageUrl(request.getImageUrl());
        item.setCategory(category);

        return toResponse(lostItemRepository.save(item));
    }

    @Transactional
    public void closeLostItem(String email, Long id) {
        User user = getUser(email);
        LostItem item = getLostItem(id);

        if (!item.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "You can close only your own lost items");
        }

        item.setStatus("CLOSED");
        lostItemRepository.save(item);
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

    private LostItem getLostItem(Long id) {
        return lostItemRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Lost item not found"));
    }

    public List<LostItemResponse> searchLostItems(
            String keyword,
            Long categoryId,
            String location,
            String status) {

        keyword = normalizeFilter(keyword);
        location = normalizeFilter(location);
        status = normalizeFilter(status);

        if (status == null) {
            status = "ACTIVE";
        }

        return lostItemRepository.searchLostItems(
                keyword, categoryId, location, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private LostItemResponse toResponse(LostItem item) {
        LostItemResponse response = new LostItemResponse();

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
        response.setLostDate(item.getLostDate());
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
