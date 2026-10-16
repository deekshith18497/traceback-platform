
package com.lostfound.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PasswordResetRequest {

    @NotBlank(message = "Channel is required")
    @Pattern(
        regexp = "(?i)EMAIL|MOBILE",
        message = "Channel must be EMAIL or MOBILE"
    )
    private String channel;

    @NotBlank(message = "Identifier is required")
    @Size(max = 255)
    private String identifier;

    @NotBlank(message = "Reset token is required")
    @Size(max = 512)
    private String resetToken;

    @NotBlank(message = "New password is required")
    @Size(
        min = 8,
        max = 72,
        message = "Password must contain 8 to 72 characters"
    )
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$",
        message = "Password must include uppercase, lowercase, number and special character"
    )
    private String newPassword;

    public PasswordResetRequest() {
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

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
