
package com.lostfound.dto.verification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VerificationRequestDto {

    @NotNull(message = "Match ID is required")
    private Long matchId;

    @NotBlank(message = "Proof description is required")
    @Size(min = 10, max = 2000,
          message = "Proof description must contain 10 to 2000 characters")
    private String proofDescription;

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public String getProofDescription() {
        return proofDescription;
    }

    public void setProofDescription(String proofDescription) {
        this.proofDescription = proofDescription;
    }
}
