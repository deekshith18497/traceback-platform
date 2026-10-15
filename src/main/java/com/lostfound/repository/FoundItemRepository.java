
package com.lostfound.repository;

import com.lostfound.entity.FoundItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoundItemRepository extends JpaRepository<FoundItem, Long> {

        List<FoundItem> findByUserIdOrderByCreatedAtDesc(Long userId);

        List<FoundItem> findByStatusIgnoreCaseOrderByCreatedAtDesc(String status);

        List<FoundItem> findByCategoryIdAndStatusIgnoreCase(
                        Long categoryId, String status);

        List<FoundItem> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        String title, String description);

       boolean existsByUserIdAndCategoryIdAndStatusIgnoreCaseAndTitleIgnoreCaseAndLocationIgnoreCase(
                        Long userId,
                        Long categoryId,
                        String status,
                        String title,
                        String location);
}
