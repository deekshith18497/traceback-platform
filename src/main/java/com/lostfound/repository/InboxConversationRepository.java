
package com.lostfound.repository;

import com.lostfound.entity.InboxConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InboxConversationRepository
        extends JpaRepository<InboxConversation, Long> {

    @Query("""
        select c from InboxConversation c
        where c.participantOne.id = :userId
           or c.participantTwo.id = :userId
        order by c.updatedAt desc
        """)
    List<InboxConversation> findInboxForUser(
            @Param("userId") Long userId);

    @Query("""
        select c from InboxConversation c
        where c.itemType = :itemType
          and c.itemId = :itemId
          and (
              (c.participantOne.id = :userA
               and c.participantTwo.id = :userB)
              or
              (c.participantOne.id = :userB
               and c.participantTwo.id = :userA)
          )
        """)
    Optional<InboxConversation> findExistingConversation(
            @Param("itemType") String itemType,
            @Param("itemId") Long itemId,
            @Param("userA") Long userA,
            @Param("userB") Long userB);
}
