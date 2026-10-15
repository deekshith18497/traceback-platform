
package com.lostfound.repository;

import com.lostfound.entity.LostItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LostItemRepository extends JpaRepository<LostItem, Long> {

    List<LostItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<LostItem> findByStatusIgnoreCaseOrderByCreatedAtDesc(String status);

    List<LostItem> findByCategoryIdAndStatusIgnoreCase(
            Long categoryId, String status);

    List<LostItem> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title, String description);

    // Existing search method: preserved for compatibility.
    @Query("""
        SELECT l FROM LostItem l
        WHERE (:keyword IS NULL OR
               LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR l.category.id = :categoryId)
          AND (:location IS NULL OR
               LOWER(l.location) LIKE LOWER(CONCAT('%', :location, '%')))
          AND (:status IS NULL OR LOWER(l.status) = LOWER(:status))
        ORDER BY l.createdAt DESC
        """)
    List<LostItem> searchLostItems(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("location") String location,
            @Param("status") String status);

    // New paginated search method.
    @Query("""
        SELECT l FROM LostItem l
        WHERE (:keyword IS NULL OR
               LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR l.category.id = :categoryId)
          AND (:location IS NULL OR
               LOWER(l.location) LIKE LOWER(CONCAT('%', :location, '%')))
          AND (:status IS NULL OR LOWER(l.status) = LOWER(:status))
        """)
    Page<LostItem> searchLostItemsPaginated(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("location") String location,
            @Param("status") String status,
            Pageable pageable);

             boolean existsByUserIdAndCategoryIdAndStatusIgnoreCaseAndTitleIgnoreCaseAndLocationIgnoreCase(
                        Long userId,
                        Long categoryId,
                        String status,
                        String title,
                        String location);

}
