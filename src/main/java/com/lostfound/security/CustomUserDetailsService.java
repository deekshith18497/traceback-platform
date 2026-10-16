package com.lostfound.security;

import com.lostfound.entity.User;
import com.lostfound.repository.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(
            UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier)
            throws UsernameNotFoundException {

        if (identifier == null || identifier.isBlank()) {
            throw new UsernameNotFoundException(
                    "Email or mobile number is required");
        }

        String normalized = normalize(identifier);

        User user = userRepository
                .findByLoginIdentifier(normalized)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Account not found"));

        String principal = getPrincipal(user);

        return org.springframework.security.core.userdetails.User
                .withUsername(principal)
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .disabled(!user.isEnabled())
                .build();
    }

    private String getPrincipal(User user) {
        if (user.getEmail() != null
                && !user.getEmail().isBlank()) {
            return user.getEmail()
                    .trim()
                    .toLowerCase(Locale.ROOT);
        }

        return user.getPhone();
    }

    private String normalize(String identifier) {
        String value = identifier.trim();

        if (value.contains("@")) {
            return value.toLowerCase(Locale.ROOT);
        }

        return value.replaceAll("[\\s()+.-]", "");
    }
}