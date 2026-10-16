package com.lostfound.security;

import com.lostfound.entity.User;
import com.lostfound.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            String identifier = jwtService.extractUsername(token);

            if (identifier != null
                    && SecurityContextHolder.getContext()
                            .getAuthentication() == null
                    && jwtService.isTokenValid(token, identifier)) {

                Optional<User> applicationUser =
                        userRepository.findByLoginIdentifier(identifier);

                if (applicationUser.isPresent()) {
                    User account = applicationUser.get();

                    if (account.isEnabled()) {
                        String principal = getPrincipal(account);

                        UserDetails userDetails =
                                org.springframework.security.core.userdetails.User
                                        .withUsername(principal)
                                        .password(account.getPassword())
                                        .authorities(
                                                "ROLE_" + account.getRole().name())
                                        .disabled(!account.isEnabled())
                                        .build();

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities());

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request));

                        SecurityContextHolder.getContext()
                                .setAuthentication(authentication);
                    }
                }
            }

        } catch (io.jsonwebtoken.JwtException
                 | IllegalArgumentException exception) {

            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
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
}