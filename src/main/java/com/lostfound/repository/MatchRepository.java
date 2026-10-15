
package com.lostfound.repository;

import com.lostfound.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByLostItemUserIdOrderByCreatedAtDesc(Long userId);

    List<Match> findByFoundItemUserIdOrderByCreatedAtDesc(Long userId);

    List<Match> findByStatusIgnoreCaseOrderByCreatedAtDesc(String status);

    List<Match> findByLostItemId(Long lostItemId);

    List<Match> findByFoundItemId(Long foundItemId);

    Optional<Match> findByLostItemIdAndFoundItemId(
            Long lostItemId, Long foundItemId);

    boolean existsByLostItemIdAndFoundItemId(
            Long lostItemId, Long foundItemId);
}
