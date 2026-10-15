
package com.lostfound.repository;

import com.lostfound.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLog> findByEventTypeOrderByCreatedAtDesc(
            String eventType,
            Pageable pageable);

    Page<AuditLog> findByActorIdentifierIgnoreCaseOrderByCreatedAtDesc(
            String actorIdentifier,
            Pageable pageable);
}
