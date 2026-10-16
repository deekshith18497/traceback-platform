
package com.lostfound.service;

import com.lostfound.exception.OtpDeliveryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OtpDeliveryService {

    private final JavaMailSender mailSender;
    private final boolean emailEnabled;
    private final boolean smsEnabled;
    private final String fromAddress;

    public OtpDeliveryService(
            JavaMailSender mailSender,
            @Value("${otp.email.enabled:false}") boolean emailEnabled,
            @Value("${otp.sms.enabled:false}") boolean smsEnabled,
            @Value("${spring.mail.username:}") String fromAddress) {

        this.mailSender = mailSender;
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
        this.fromAddress = fromAddress;
    }

    public void sendEmailOtp(String email, String otp) {
        if (!emailEnabled || fromAddress.isBlank()) {
            throw new OtpDeliveryException(
                "Email OTP delivery is not configured"
            );
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(email);
            message.setSubject("HERO FIND verification code");
            message.setText(
                "Your HERO FIND verification code is " + otp
                + ". It expires in 5 minutes. Do not share this code."
            );

            mailSender.send(message);
        } catch (Exception exception) {
            throw new OtpDeliveryException(
                "Unable to send verification email", exception
            );
        }
    }

    public void sendSmsOtp(String phone, String otp) {
        if (!smsEnabled) {
            throw new OtpDeliveryException(
                "SMS OTP delivery is not configured"
            );
        }

        // Do not silently claim SMS was sent.
        // Connect your configured SMS provider here before enabling SMS.
        throw new OtpDeliveryException(
            "SMS provider integration is not implemented yet"
        );
    }
}
