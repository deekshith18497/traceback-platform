
package com.lostfound.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "watchlist_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_watchlist_user_item",
        columnNames = {"user_id", "item_type", "item_id"}
    )
)
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "item_type", nullable = false, length = 10)
    private String itemType;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime savedAt;

    public WatchlistItem() {
    }

    @PrePersist
    protected void onCreate() {
        savedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }
}
