
package com.lostfound.dto.inbox;

import java.time.LocalDateTime;

public class ConversationResponse {

    private Long id;
    private Long otherUserId;
    private String otherUserName;
    private String itemType;
    private Long itemId;
    private String itemTitle;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOtherUserId() { return otherUserId; }
    public void setOtherUserId(Long otherUserId) {
        this.otherUserId = otherUserId;
    }

    public String getOtherUserName() { return otherUserName; }
    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getItemTitle() { return itemTitle; }
    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
