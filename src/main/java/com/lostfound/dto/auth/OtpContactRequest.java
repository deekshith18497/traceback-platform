
package com.lostfound.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OtpContactRequest {

    @NotBlank(message = "Channel is required")
    @Pattern(
        regexp = "(?i)EMAIL|MOBILE",
        message = "Channel must be EMAIL or MOBILE"
    )
    private String channel;

    @NotBlank(message = "Identifier is required")
    @Size(max = 255, message = "Identifier is too long")
    private String identifier;

    public OtpContactRequest() {
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
