
package com.lostfound.repository;

import com.lostfound.entity.VerificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationRequestRepository
        extends JpaRepository<VerificationRequest, Long> {

    List<VerificationRequest> findByRequesterIdOrderByCreatedAtDesc(
            Long requesterId);

    List<VerificationRequest> findByStatusIgnoreCaseOrderByCreatedAtDesc(
            String status);

    List<VerificationRequest> findByMatchIdOrderByCreatedAtDesc(
            Long matchId);
}
