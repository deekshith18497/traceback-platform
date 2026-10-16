
package com.lostfound.dto.auth;

public class PasswordResetVerificationResponse {

    private String message;
    private String resetToken;
    private long expiresInSeconds;

    public PasswordResetVerificationResponse() {
    }

    public PasswordResetVerificationResponse(
            String message,
            String resetToken,
            long expiresInSeconds) {
        this.message = message;
        this.resetToken = resetToken;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}
