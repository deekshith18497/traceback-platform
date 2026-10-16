
package com.lostfound.service;

import com.lostfound.dto.auth.AuthResponse;
import com.lostfound.dto.auth.OtpContactRequest;
import com.lostfound.dto.auth.OtpVerificationRequest;
import com.lostfound.dto.auth.PasswordResetRequest;
import com.lostfound.dto.auth.PasswordResetVerificationResponse;
import com.lostfound.dto.auth.RegistrationOtpRequest;
import com.lostfound.entity.OtpChallenge;
import com.lostfound.entity.User;
import com.lostfound.repository.OtpChallengeRepository;
import com.lostfound.repository.UserRepository;
import com.lostfound.security.JwtService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class OtpAuthService {

    private static final String REGISTRATION = "REGISTRATION";
    private static final String PASSWORD_RESET = "PASSWORD_RESET";

    private static final int OTP_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 5;
    private static final int OTP_TTL_MINUTES = 5;
    private static final int RESEND_WAIT_SECONDS = 60;
    private static final int RESET_TOKEN_TTL_MINUTES = 10;

    private final OtpChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpDeliveryService deliveryService;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${otp.ttl-minutes:5}")
    private int configuredOtpTtlMinutes;

    @Value("${otp.max-attempts:5}")
    private int configuredMaxAttempts;

    @Value("${otp.resend-wait-seconds:60}")
    private int configuredResendWaitSeconds;

    public OtpAuthService(
            OtpChallengeRepository challengeRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OtpDeliveryService deliveryService,
            JwtService jwtService) {

        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.deliveryService = deliveryService;
        this.jwtService = jwtService;
    }

    // ---------------------------------------------------------
    // REGISTRATION: REQUEST OTP
    // ---------------------------------------------------------

    public String requestRegistrationOtp(RegistrationOtpRequest request) {

        String channel = normalizeChannel(request.getChannel());
        String identifier;
        String email = null;
        String phone = null;

        if ("EMAIL".equals(channel)) {
            if (request.getEmail() == null
                    || request.getEmail().isBlank()) {
                throw badRequest("Email is required.");
            }

            email = normalizeEmail(request.getEmail());
            identifier = email;

            if (userRepository.existsByEmailIgnoreCase(email)) {
                throw conflict("An account already exists with this email.");
            }

        } else {
            if (request.getPhone() == null
                    || request.getPhone().isBlank()) {
                throw badRequest("Mobile number is required.");
            }

            phone = normalizePhone(request.getPhone());
            identifier = phone;

            if (userRepository.existsByPhone(phone)) {
                throw conflict("An account already exists with this mobile number.");
            }
        }

        if (request.getName() == null
                || request.getName().isBlank()) {
            throw badRequest("Name is required.");
        }

        if (request.getPassword() == null
                || request.getPassword().length() < 8) {
            throw badRequest("Password must contain at least 8 characters.");
        }

        ensureResendAllowed(REGISTRATION, channel, identifier);

        String otp = generateOtp();

        OtpChallenge challenge = new OtpChallenge();
        challenge.setPurpose(REGISTRATION);
        challenge.setChannel(channel);
        challenge.setIdentifier(identifier);
        challenge.setOtpHash(passwordEncoder.encode(otp));
        challenge.setExpiresAt(
                LocalDateTime.now().plusMinutes(getOtpTtlMinutes()));
        challenge.setLastSentAt(LocalDateTime.now());
        challenge.setAttempts(0);
        challenge.setConsumed(false);

        challenge.setName(request.getName().trim());
        challenge.setEmail(email);
        challenge.setPhone(phone);
        challenge.setPasswordHash(
                passwordEncoder.encode(request.getPassword()));

        challengeRepository.save(challenge);

        try {
            deliverOtp(channel, identifier, otp);
        } catch (RuntimeException exception) {
            challenge.setConsumed(true);
            challengeRepository.save(challenge);
            throw exception;
        }

        return "If delivery is configured, a verification code has been sent. "
                + "The code expires in " + getOtpTtlMinutes() + " minutes.";
    }

    // ---------------------------------------------------------
    // REGISTRATION: VERIFY OTP AND CREATE ACCOUNT
    // ---------------------------------------------------------

    public AuthResponse verifyRegistrationOtp(
            OtpVerificationRequest request) {

        String channel = normalizeChannel(request.getChannel());
        String identifier = normalizeIdentifier(
                channel, request.getIdentifier());

        OtpChallenge challenge = getLatestActiveChallenge(
                REGISTRATION, channel, identifier);

        verifyOtp(challenge, request.getOtp());

        if (("EMAIL".equals(channel)
                && userRepository.existsByEmailIgnoreCase(identifier))
                || ("MOBILE".equals(channel)
                && userRepository.existsByPhone(identifier))) {

            challenge.setConsumed(true);
            challengeRepository.save(challenge);

            throw conflict("An account already exists for this identifier.");
        }

        User user = new User();
        user.setName(challenge.getName());
        user.setEmail(challenge.getEmail());
        user.setPhone(challenge.getPhone());
        user.setPassword(challenge.getPasswordHash());
        user.setRole(User.Role.USER);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        challenge.setConsumed(true);
        challenge.setOtpHash(nullSafeConsumedHash(challenge));
        challengeRepository.save(challenge);

        /*
         * Existing JwtService uses a string subject.
         * Email-only accounts use email; mobile-only accounts use phone.
         * The JWT filter and UserDetailsService must support both subjects
         * before mobile-only login is considered complete.
         */
        String subject = savedUser.getEmail() != null
                ? savedUser.getEmail()
                : savedUser.getPhone();

        String token = jwtService.generateToken(subject);

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                savedUser.getRole().name()
        );
    }

    // ---------------------------------------------------------
    // PASSWORD RESET: REQUEST OTP
    // ---------------------------------------------------------

    public String requestPasswordResetOtp(OtpContactRequest request) {

        String channel = normalizeChannel(request.getChannel());
        String identifier = normalizeIdentifier(
                channel, request.getIdentifier());

        Optional<User> account = findUser(channel, identifier);

        /*
         * Use a generic response to avoid revealing whether an account
         * exists. No OTP is generated or delivered for unknown accounts.
         */
        if (account.isEmpty()) {
            return "If an account matches those details, a verification code "
                    + "will be sent.";
        }

        ensureResendAllowed(PASSWORD_RESET, channel, identifier);

        String otp = generateOtp();

        OtpChallenge challenge = new OtpChallenge();
        challenge.setPurpose(PASSWORD_RESET);
        challenge.setChannel(channel);
        challenge.setIdentifier(identifier);
        challenge.setOtpHash(passwordEncoder.encode(otp));
        challenge.setExpiresAt(
                LocalDateTime.now().plusMinutes(getOtpTtlMinutes()));
        challenge.setLastSentAt(LocalDateTime.now());
        challenge.setAttempts(0);
        challenge.setConsumed(false);

        challengeRepository.save(challenge);

        try {
            deliverOtp(channel, identifier, otp);
        } catch (RuntimeException exception) {
            challenge.setConsumed(true);
            challengeRepository.save(challenge);
            throw exception;
        }

        return "If an account matches those details, a verification code "
                + "will be sent.";
    }

    // ---------------------------------------------------------
    // PASSWORD RESET: VERIFY OTP AND ISSUE RESET TOKEN
    // ---------------------------------------------------------

    public PasswordResetVerificationResponse verifyPasswordResetOtp(
            OtpVerificationRequest request) {

        String channel = normalizeChannel(request.getChannel());
        String identifier = normalizeIdentifier(
                channel, request.getIdentifier());

        OtpChallenge challenge = getLatestActiveChallenge(
                PASSWORD_RESET, channel, identifier);

        verifyOtp(challenge, request.getOtp());

        if (findUser(channel, identifier).isEmpty()) {
            challenge.setConsumed(true);
            challengeRepository.save(challenge);
            throw badRequest("The account could not be verified.");
        }

        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);

        String resetToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);

        challenge.setConsumed(true);
        challenge.setResetTokenHash(sha256(resetToken));
        challenge.setResetTokenExpiresAt(
                LocalDateTime.now().plusMinutes(RESET_TOKEN_TTL_MINUTES));
        challengeRepository.save(challenge);

        PasswordResetVerificationResponse response =
                new PasswordResetVerificationResponse();

        response.setMessage(
                "Verification successful. Use the reset token to set a new password.");
        response.setResetToken(resetToken);
        response.setExpiresInSeconds(RESET_TOKEN_TTL_MINUTES * 60);

        return response;
    }

    // ---------------------------------------------------------
    // PASSWORD RESET: CONSUME RESET TOKEN
    // ---------------------------------------------------------

    public String resetPassword(PasswordResetRequest request) {

        String channel = normalizeChannel(request.getChannel());
        String identifier = normalizeIdentifier(
                channel, request.getIdentifier());

        if (request.getNewPassword() == null
                || request.getNewPassword().length() < 8) {
            throw badRequest("New password must contain at least 8 characters.");
        }

        String resetToken = request.getResetToken();

        if (resetToken == null || resetToken.isBlank()) {
            throw badRequest("Reset token is required.");
        }

        OtpChallenge challenge = challengeRepository
                .findByPurposeAndChannelAndIdentifierOrderByCreatedAtDesc(
                        PASSWORD_RESET, channel, identifier)
                .stream()
                .filter(item -> item.getResetTokenHash() != null)
                .findFirst()
                .orElseThrow(() -> badRequest(
                        "Invalid or expired password reset token."));

        if (challenge.getResetTokenExpiresAt() == null
                || !challenge.getResetTokenExpiresAt().isAfter(
                        LocalDateTime.now())) {
            challenge.setResetTokenHash(null);
            challenge.setResetTokenExpiresAt(null);
            challengeRepository.save(challenge);

            throw badRequest("Invalid or expired password reset token.");
        }

        String suppliedHash = sha256(resetToken);

        if (!MessageDigest.isEqual(
                challenge.getResetTokenHash().getBytes(StandardCharsets.UTF_8),
                suppliedHash.getBytes(StandardCharsets.UTF_8))) {
            throw badRequest("Invalid or expired password reset token.");
        }

        User user = findUser(channel, identifier)
                .orElseThrow(() -> badRequest("Account not found."));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Clear token material after successful use: it cannot be reused.
        challenge.setResetTokenHash(null);
        challenge.setResetTokenExpiresAt(null);
        challengeRepository.save(challenge);

        return "Password has been reset successfully.";
    }

    // ---------------------------------------------------------
    // INTERNAL HELPERS
    // ---------------------------------------------------------

    private void ensureResendAllowed(
            String purpose, String channel, String identifier) {

        challengeRepository
                .findTopByPurposeAndChannelAndIdentifierAndConsumedFalseOrderByCreatedAtDesc(
                        purpose, channel, identifier)
                .ifPresent(existing -> {
                    LocalDateTime allowedAt = existing.getLastSentAt()
                            .plusSeconds(getResendWaitSeconds());

                    if (LocalDateTime.now().isBefore(allowedAt)) {
                        throw new ResponseStatusException(
                                HttpStatus.TOO_MANY_REQUESTS,
                                "Please wait before requesting another code.");
                    }
                });
    }

    private OtpChallenge getLatestActiveChallenge(
            String purpose, String channel, String identifier) {

        OtpChallenge challenge = challengeRepository
                .findTopByPurposeAndChannelAndIdentifierAndConsumedFalseOrderByCreatedAtDesc(
                        purpose, channel, identifier)
                .orElseThrow(() -> badRequest(
                        "No active verification request was found. Request a new code."));

        if (!challenge.getExpiresAt().isAfter(LocalDateTime.now())) {
            challenge.setConsumed(true);
            challengeRepository.save(challenge);
            throw badRequest("The verification code has expired. Request a new one.");
        }

        if (challenge.getAttempts() >= getMaxAttempts()) {
            challenge.setConsumed(true);
            challengeRepository.save(challenge);
            throw badRequest("Too many incorrect attempts. Request a new code.");
        }

        return challenge;
    }

    private void verifyOtp(OtpChallenge challenge, String otp) {

        if (otp == null || !otp.matches("\\d{6}")) {
            recordFailedAttempt(challenge);
            throw badRequest("Invalid verification code.");
        }

        if (challenge.getOtpHash() == null
                || !passwordEncoder.matches(otp, challenge.getOtpHash())) {
            recordFailedAttempt(challenge);
            throw badRequest("Invalid verification code.");
        }
    }

    private void recordFailedAttempt(OtpChallenge challenge) {
        int attempts = challenge.getAttempts() + 1;
        challenge.setAttempts(attempts);

        if (attempts >= getMaxAttempts()) {
            challenge.setConsumed(true);
        }

        challengeRepository.save(challenge);
    }

    private void deliverOtp(String channel, String identifier, String otp) {
        if ("EMAIL".equals(channel)) {
            deliveryService.sendEmailOtp(identifier, otp);
        } else {
            deliveryService.sendSmsOtp(identifier, otp);
        }
    }

    private Optional<User> findUser(String channel, String identifier) {
        if ("EMAIL".equals(channel)) {
            return userRepository.findByEmailIgnoreCase(identifier);
        }

        return userRepository.findByPhone(identifier);
    }

    private String normalizeChannel(String channel) {
        if (channel == null) {
            throw badRequest("Channel must be EMAIL or MOBILE.");
        }

        String normalized = channel.trim().toUpperCase(Locale.ROOT);

        if (!normalized.equals("EMAIL") && !normalized.equals("MOBILE")) {
            throw badRequest("Channel must be EMAIL or MOBILE.");
        }

        return normalized;
    }

    private String normalizeIdentifier(String channel, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw badRequest("Identifier is required.");
        }

        return "EMAIL".equals(channel)
                ? normalizeEmail(identifier)
                : normalizePhone(identifier);
    }

    private String normalizeEmail(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);

        if (!normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw badRequest("Enter a valid email address.");
        }

        return normalized;
    }

    /*
     * Canonical mobile format is +91 followed by 10 digits.
     * Existing records should use the same format for consistent lookups.
     */
    private String normalizePhone(String phone) {
        String digits = phone.trim().replaceAll("[\\s()-]", "");

        if (digits.startsWith("+91")) {
            digits = digits.substring(3);
        } else if (digits.startsWith("91") && digits.length() == 12) {
            digits = digits.substring(2);
        }

        if (!digits.matches("[6-9]\\d{9}")) {
            throw badRequest(
                    "Enter a valid 10-digit Indian mobile number.");
        }

        return "+91" + digits;
    }

    private String generateOtp() {
        return String.format(
                Locale.ROOT,
                "%06d",
                secureRandom.nextInt(1_000_000));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to securely process reset token.", exception);
        }
    }

    private String nullSafeConsumedHash(OtpChallenge challenge) {
        /*
         * The entity currently declares otpHash nullable=false.
         * Keep the consumed challenge valid in the database while
         * preventing it from being used again.
         */
        return challenge.getOtpHash();
    }

    private int getOtpTtlMinutes() {
        return configuredOtpTtlMinutes > 0
                ? configuredOtpTtlMinutes
                : OTP_TTL_MINUTES;
    }

    private int getMaxAttempts() {
        return configuredMaxAttempts > 0
                ? configuredMaxAttempts
                : MAX_ATTEMPTS;
    }

    private int getResendWaitSeconds() {
        return configuredResendWaitSeconds > 0
                ? configuredResendWaitSeconds
                : RESEND_WAIT_SECONDS;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
