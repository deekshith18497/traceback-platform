
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

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        // Treat missing or blank phone numbers as null.
        String phone = request.getPhone();

        if (phone != null) {
            phone = phone.trim();

            if (phone.isEmpty()) {
                phone = null;
            }
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email is already registered");
        }

        // Check duplicate phone numbers only when provided.
        if (phone != null && userRepository.existsByPhone(phone)) {
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

        String token =
                jwtService.generateToken(savedUser.getEmail());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }

    public AuthResponse login(LoginRequest request) {

        String identifier = request.getEmail().trim();

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

        User user;

        // Support both email and mobile number login.
        if (identifier.contains("@")) {
            String email = identifier.toLowerCase();

            user = userRepository.findByEmail(email)
                    .orElse(null);
        } else {
            user = userRepository.findByPhone(identifier)
                    .orElse(null);
        }

        // Record failed attempts for unknown accounts too.
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
            throw new IllegalArgumentException(
                    "Account is disabled");
        }

        // Authenticate using email because Spring Security
        // uses email as the username.
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getEmail(),
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

        // Reset failed attempts after successful authentication.
        loginAttemptService.reset(identifier);

        recordLoginEvent(
                "LOGIN_SUCCESS",
                identifier,
                "User authenticated successfully"
        );

        String token =
                jwtService.generateToken(user.getEmail());

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    /**
     * Save audit events without allowing an audit-storage
     * failure to directly interrupt authentication.
     *
     * Never record passwords or JWT tokens.
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
