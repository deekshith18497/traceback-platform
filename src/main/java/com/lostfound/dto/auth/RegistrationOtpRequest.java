
package com.lostfound.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrationOtpRequest {

    @NotBlank(message = "Channel is required")
    @Pattern(
        regexp = "(?i)EMAIL|MOBILE",
        message = "Channel must be EMAIL or MOBILE"
    )
    private String channel;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @Email(message = "Enter a valid email address")
    @Size(max = 255)
    private String email;

    @Pattern(
        regexp = "^(?:\\+91[6-9]\\d{9}|[6-9]\\d{9})$",
        message = "Enter a valid Indian mobile number"
    )
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$",
        message = "Password must include uppercase, lowercase, number and special character"
    )
    private String password;

    public RegistrationOtpRequest() {
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
