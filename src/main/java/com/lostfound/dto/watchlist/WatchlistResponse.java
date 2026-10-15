
package com.lostfound.dto.watchlist;

import java.time.LocalDateTime;

public record WatchlistResponse(
        Long id,
        Long itemId,
        String itemType,
        String title,
        String description,
        String location,
        Double latitude,
        Double longitude,
        String imageUrl,
        String status,
        Long categoryId,
        String categoryName,
        LocalDateTime lostDate,
        LocalDateTime foundDate,
        LocalDateTime createdAt,
        LocalDateTime savedAt,
        Long userId,
        String userName
) {
}
