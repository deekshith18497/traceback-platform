
package com.lostfound.dto.admin;

public class AdminDashboardResponse {

    private long totalUsers;
    private long enabledUsers;
    private long totalLostItems;
    private long totalFoundItems;
    private long totalMatches;
    private long pendingMatches;
    private long confirmedMatches;

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long value) {
        this.totalUsers = value;
    }

    public long getEnabledUsers() {
        return enabledUsers;
    }

    public void setEnabledUsers(long value) {
        this.enabledUsers = value;
    }

    public long getTotalLostItems() {
        return totalLostItems;
    }

    public void setTotalLostItems(long value) {
        this.totalLostItems = value;
    }

    public long getTotalFoundItems() {
        return totalFoundItems;
    }

    public void setTotalFoundItems(long value) {
        this.totalFoundItems = value;
    }

    public long getTotalMatches() {
        return totalMatches;
    }

    public void setTotalMatches(long value) {
        this.totalMatches = value;
    }

    public long getPendingMatches() {
        return pendingMatches;
    }

    public void setPendingMatches(long value) {
        this.pendingMatches = value;
    }

    public long getConfirmedMatches() {
        return confirmedMatches;
    }

    public void setConfirmedMatches(long value) {
        this.confirmedMatches = value;
    }
}
