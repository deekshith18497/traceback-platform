
package com.lostfound.dto.inbox;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StartConversationRequest {

    @NotBlank
    private String itemType;

    @NotNull
    private Long itemId;

    @NotBlank
    @Size(max = 2000)
    private String message;

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getMessage() { return message; }
    public void setMessage(String message) {
        this.message = message;
    }
}
