
package com.lostfound.dto.inbox;

import java.time.LocalDateTime;

public class MessageResponse {

    private Long id;
    private Long senderId;
    private String senderName;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getContent() { return content; }
    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
