
package com.lostfound.dto.match;

import java.util.ArrayList;
import java.util.List;

public class MatchReasonResponse {

    private Long matchId;
    private Double similarityScore;
    private List<String> reasons = new ArrayList<>();

    public MatchReasonResponse() {
    }

    public MatchReasonResponse(Long matchId, Double similarityScore,
                               List<String> reasons) {
        this.matchId = matchId;
        this.similarityScore = similarityScore;
        if (reasons != null) {
            this.reasons = new ArrayList<>(reasons);
        }
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(Double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons == null
                ? new ArrayList<>()
                : new ArrayList<>(reasons);
    }
}
