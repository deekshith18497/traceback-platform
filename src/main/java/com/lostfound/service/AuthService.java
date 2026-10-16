
package com.lostfound.service;

import com.lostfound.dto.auth.AuthResponse;
import com.lostfound.dto.auth.LoginRequest;
import com.lostfound.dto.auth.RegisterRequest;
import com.lostfound.entity.User;
import com.lostfound.repository.UserRepository;
import com.lostfound.security.JwtService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final AuditLogService auditLogService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            LoginAttemptService loginAttemptService,
            AuditLogService auditLogService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
        this.auditLogService = auditLogService;
    }

    /**
     * Legacy direct registration method.
     * The public controller should continue requiring OTP
     * verification before registration.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (request == null
                || request.getEmail() == null
                || request.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "Email is required for direct registration");
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        String phone = normalizePhoneForStorage(
                request.getPhone());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "Email is already registered");
        }

        if (phone != null
                && userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException(
                    "Phone number is already registered");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(
                passwordEncoder.encode(request.getPassword()));
        user.setPhone(phone);
        user.setRole(User.Role.USER);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(
                savedUser.getEmail());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                savedUser.getRole().name()
        );
    }

    /**
     * Supports login using either email or mobile number.
     *
     * The authenticated principal and JWT subject must match
     * the principal created by CustomUserDetailsService.
     */
    public AuthResponse login(LoginRequest request) {

        if (request == null
                || request.getEmail() == null
                || request.getEmail().isBlank()
                || request.getPassword() == null) {
            throw new IllegalArgumentException(
                    "Email/mobile number and password are required");
        }

        String identifier = normalizeLoginIdentifier(
                request.getEmail());

        // Check whether this identifier is temporarily locked.
        try {
            loginAttemptService.checkBlocked(identifier);
        } catch (IllegalArgumentException exception) {

            recordLoginEvent(
                    "LOGIN_BLOCKED",
                    identifier,
                    "Login rejected due to repeated failed attempts"
            );

            throw exception;
        }

        // Repository lookup supports email and phone formats.
        User user = userRepository
                .findByLoginIdentifier(identifier)
                .orElse(null);

        if (user == null) {

            loginAttemptService.recordFailure(identifier);

            recordLoginEvent(
                    "LOGIN_FAILED",
                    identifier,
                    "Invalid credentials"
            );

            throw new IllegalArgumentException(
                    "Invalid email/mobile number or password");
        }

        if (!user.isEnabled()) {
            recordLoginEvent(
                    "LOGIN_FAILED",
                    identifier,
                    "Login rejected because the account is disabled"
            );

            throw new IllegalArgumentException(
                    "Account is disabled");
        }

        /*
         * Email accounts authenticate using email.
         * Mobile-only accounts authenticate using their stored phone.
         * This matches CustomUserDetailsService's principal selection.
         */
        String principal = getAuthenticationPrincipal(user);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            request.getPassword()
                    )
            );

        } catch (AuthenticationException exception) {

            loginAttemptService.recordFailure(identifier);

            recordLoginEvent(
                    "LOGIN_FAILED",
                    identifier,
                    "Invalid credentials"
            );

            throw new IllegalArgumentException(
                    "Invalid email/mobile number or password");
        }

        // Clear failed attempts only after successful authentication.
        loginAttemptService.reset(identifier);

        recordLoginEvent(
                "LOGIN_SUCCESS",
                identifier,
                "User authenticated successfully"
        );

        // JWT subject must match the principal used by the filter.
        String token = jwtService.generateToken(principal);

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name()
        );
    }

    /**
     * Uses the same principal convention as CustomUserDetailsService:
     * email when available, otherwise phone.
     */
    private String getAuthenticationPrincipal(User user) {

        if (user.getEmail() != null
                && !user.getEmail().isBlank()) {
            return user.getEmail().trim()
                    .toLowerCase(Locale.ROOT);
        }

        if (user.getPhone() != null
                && !user.getPhone().isBlank()) {
            return user.getPhone().trim();
        }

        throw new IllegalArgumentException(
                "Account has no valid login identifier");
    }

    /**
     * Normalize identifiers for login lookup and throttling.
     * Indian mobile numbers become +91XXXXXXXXXX.
     */
    private String normalizeLoginIdentifier(String rawIdentifier) {

        String value = rawIdentifier.trim();

        if (value.contains("@")) {
            return value.toLowerCase(Locale.ROOT);
        }

        String digits = value.replaceAll("\\D", "");

        if (digits.length() == 10
                && digits.matches("[6-9]\\d{9}")) {
            return "+91" + digits;
        }

        if (digits.length() == 12
                && digits.startsWith("91")
                && digits.charAt(2) >= '6'
                && digits.charAt(2) <= '9') {
            return "+" + digits;
        }

        return value;
    }

    /**
     * Normalize optional phone numbers for legacy direct registration.
     */
    private String normalizePhoneForStorage(String rawPhone) {

        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }

        String value = rawPhone.trim();
        String digits = value.replaceAll("\\D", "");

        if (digits.length() == 10
                && digits.matches("[6-9]\\d{9}")) {
            return "+91" + digits;
        }

        if (digits.length() == 12
                && digits.startsWith("91")
                && digits.charAt(2) >= '6'
                && digits.charAt(2) <= '9') {
            return "+" + digits;
        }

        return value;
    }

    /**
     * Record audit events without exposing passwords or JWTs.
     */
    private void recordLoginEvent(
            String eventType,
            String identifier,
            String details) {

        try {
            auditLogService.recordEvent(
                    eventType,
                    identifier,
                    details,
                    null
            );

        } catch (Exception exception) {
            System.err.println(
                    "Unable to save security audit event: "
                            + eventType);
        }
    }
}
