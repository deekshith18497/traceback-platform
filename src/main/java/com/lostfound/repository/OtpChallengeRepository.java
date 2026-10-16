
package com.lostfound.repository;

import com.lostfound.entity.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OtpChallengeRepository
        extends JpaRepository<OtpChallenge, Long> {

    Optional<OtpChallenge>
    findTopByPurposeAndChannelAndIdentifierAndConsumedFalseOrderByCreatedAtDesc(
            String purpose,
            String channel,
            String identifier);

    List<OtpChallenge>
    findByPurposeAndChannelAndIdentifierOrderByCreatedAtDesc(
            String purpose,
            String channel,
            String identifier);
}
