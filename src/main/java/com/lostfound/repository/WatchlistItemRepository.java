
package com.lostfound.repository;

import com.lostfound.entity.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistItemRepository
        extends JpaRepository<WatchlistItem, Long> {

    List<WatchlistItem> findAllByUserIdOrderBySavedAtDesc(
            Long userId);

    Optional<WatchlistItem> findByUserIdAndItemTypeAndItemId(
            Long userId,
            String itemType,
            Long itemId);

    Optional<WatchlistItem> findByIdAndUserId(
            Long id,
            Long userId);
}
