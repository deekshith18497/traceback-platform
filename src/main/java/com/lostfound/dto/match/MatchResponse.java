
package com.lostfound.dto.match;

import java.time.LocalDateTime;

public class MatchResponse {

    private Long id;
    private Long lostItemId;
    private String lostItemTitle;
    private Long foundItemId;
    private String foundItemTitle;
    private Double similarityScore;
    private String matchReason;
    private String status;
    private LocalDateTime createdAt;

    public MatchResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLostItemId() {
        return lostItemId;
    }

    public void setLostItemId(Long lostItemId) {
        this.lostItemId = lostItemId;
    }

    public String getLostItemTitle() {
        return lostItemTitle;
    }

    public void setLostItemTitle(String lostItemTitle) {
        this.lostItemTitle = lostItemTitle;
    }

    public Long getFoundItemId() {
        return foundItemId;
    }

    public void setFoundItemId(Long foundItemId) {
        this.foundItemId = foundItemId;
    }

    public String getFoundItemTitle() {
        return foundItemTitle;
    }

    public void setFoundItemTitle(String foundItemTitle) {
        this.foundItemTitle = foundItemTitle;
    }

    public Double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(Double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public String getMatchReason() {
        return matchReason;
    }

    public void setMatchReason(String matchReason) {
        this.matchReason = matchReason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
