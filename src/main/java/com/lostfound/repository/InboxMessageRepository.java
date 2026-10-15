
package com.lostfound.repository;

import com.lostfound.entity.InboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InboxMessageRepository
        extends JpaRepository<InboxMessage, Long> {

    List<InboxMessage> findByConversationIdOrderByCreatedAtAsc(
            Long conversationId);

    List<InboxMessage>
    findByConversationIdAndSenderIdNotAndReadAtIsNull(
            Long conversationId, Long senderId);

    long countByConversationIdAndSenderIdNotAndReadAtIsNull(
            Long conversationId, Long senderId);
}
