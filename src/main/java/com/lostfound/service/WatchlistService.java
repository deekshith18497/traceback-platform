
package com.lostfound.service;

import com.lostfound.dto.watchlist.WatchlistRequest;
import com.lostfound.dto.watchlist.WatchlistResponse;
import com.lostfound.entity.FoundItem;
import com.lostfound.entity.LostItem;
import com.lostfound.entity.User;
import com.lostfound.entity.WatchlistItem;
import com.lostfound.repository.FoundItemRepository;
import com.lostfound.repository.LostItemRepository;
import com.lostfound.repository.UserRepository;
import com.lostfound.repository.WatchlistItemRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class WatchlistService {

        private final WatchlistItemRepository watchlistRepository;
        private final UserRepository userRepository;
        private final LostItemRepository lostItemRepository;
        private final FoundItemRepository foundItemRepository;

        public WatchlistService(
                        WatchlistItemRepository watchlistRepository,
                        UserRepository userRepository,
                        LostItemRepository lostItemRepository,
                        FoundItemRepository foundItemRepository) {
                this.watchlistRepository = watchlistRepository;
                this.userRepository = userRepository;
                this.lostItemRepository = lostItemRepository;
                this.foundItemRepository = foundItemRepository;
        }

        @Transactional
        public WatchlistResponse saveItem(
                        String identifier, WatchlistRequest request) {
                User user = getUser(identifier);
                String itemType = normalizeItemType(request.getItemType());
                Long itemId = request.getItemId();

                if (itemId == null) {
                        throw new IllegalArgumentException("Item ID is required");
                }

                if ("LOST".equals(itemType)) {
                        if (!lostItemRepository.existsById(itemId)) {
                                throw new IllegalArgumentException("Lost item not found");
                        }
                } else {
                        if (!foundItemRepository.existsById(itemId)) {
                                throw new IllegalArgumentException("Found item not found");
                        }
                }

                var existing = watchlistRepository
                                .findByUserIdAndItemTypeAndItemId(
                                                user.getId(), itemType, itemId);

                if (existing.isPresent()) {
                        return toResponse(existing.get());
                }

                WatchlistItem saved = new WatchlistItem();
                saved.setUser(user);
                saved.setItemType(itemType);
                saved.setItemId(itemId);

                return toResponse(watchlistRepository.save(saved));
        }

        @Transactional(readOnly = true)
        public List<WatchlistResponse> getMyWatchlist(String identifier) {
                User user = getUser(identifier);

                return watchlistRepository
                                .findAllByUserIdOrderBySavedAtDesc(user.getId())
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional
        public void removeItem(String identifier, Long watchlistItemId) {
                User user = getUser(identifier);

                WatchlistItem saved = watchlistRepository
                                .findByIdAndUserId(watchlistItemId, user.getId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Saved item not found in your watchlist"));

                watchlistRepository.delete(saved);
        }

        private User getUser(String identifier) {
                if (identifier == null || identifier.isBlank()) {
                        throw new IllegalArgumentException("User identifier is required");
                }

                return userRepository.findByLoginIdentifier(identifier.trim())
                                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        }

        private String normalizeItemType(String itemType) {
                if (itemType == null) {
                        throw new IllegalArgumentException(
                                        "Item type must be LOST or FOUND");
                }

                String normalized = itemType.trim().toUpperCase(Locale.ROOT);

                if (!normalized.equals("LOST") && !normalized.equals("FOUND")) {
                        throw new IllegalArgumentException(
                                        "Item type must be LOST or FOUND");
                }

                return normalized;
        }

        private WatchlistResponse toResponse(WatchlistItem saved) {
                if ("LOST".equals(saved.getItemType())) {
                        LostItem item = lostItemRepository
                                        .findById(saved.getItemId())
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "The saved lost item no longer exists"));

                        return new WatchlistResponse(
                                        saved.getId(),
                                        item.getId(),
                                        "LOST",
                                        item.getTitle(),
                                        item.getDescription(),
                                        item.getLocation(),
                                        item.getLatitude(),
                                        item.getLongitude(),
                                        item.getImageUrl(),
                                        item.getStatus(),
                                        item.getCategory() == null
                                                        ? null
                                                        : item.getCategory().getId(),
                                        item.getCategory() == null
                                                        ? null
                                                        : item.getCategory().getName(),
                                        item.getLostDate(),
                                        null,
                                        item.getCreatedAt(),
                                        saved.getSavedAt(),
                                        item.getUser().getId(),
                                        item.getUser().getName());
                }

                FoundItem item = foundItemRepository
                                .findById(saved.getItemId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "The saved found item no longer exists"));

                return new WatchlistResponse(
                                saved.getId(),
                                item.getId(),
                                "FOUND",
                                item.getTitle(),
                                item.getDescription(),
                                item.getLocation(),
                                item.getLatitude(),
                                item.getLongitude(),
                                item.getImageUrl(),
                                item.getStatus(),
                                item.getCategory() == null
                                                ? null
                                                : item.getCategory().getId(),
                                item.getCategory() == null
                                                ? null
                                                : item.getCategory().getName(),
                                null,
                                item.getFoundDate(),
                                item.getCreatedAt(),
                                saved.getSavedAt(),
                                item.getUser().getId(),
                                item.getUser().getName());
        }
}
