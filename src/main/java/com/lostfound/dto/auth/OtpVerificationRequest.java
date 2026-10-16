
package com.lostfound.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OtpVerificationRequest {

    @NotBlank(message = "Channel is required")
    @Pattern(
        regexp = "(?i)EMAIL|MOBILE",
        message = "Channel must be EMAIL or MOBILE"
    )
    private String channel;

    @NotBlank(message = "Identifier is required")
    @Size(max = 255)
    private String identifier;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "\\d{6}", message = "OTP must contain 6 digits")
    private String otp;

    public OtpVerificationRequest() {
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

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
