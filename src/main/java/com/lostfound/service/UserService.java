
package com.lostfound.service;

import com.lostfound.dto.user.ChangePasswordRequest;
import com.lostfound.dto.user.UpdateProfileRequest;
import com.lostfound.dto.user.UserResponse;
import com.lostfound.entity.User;
import com.lostfound.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse getProfile(String email) {
        User user = getUserByEmail(email);
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(
            String email, UpdateProfileRequest request) {

        User user = getUserByEmail(email);

        user.setName(request.getName().trim());
        user.setPhone(request.getPhone());

        if (request.getProfileImage() != null) {
            user.setProfileImage(request.getProfileImage());
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(
            String email, ChangePasswordRequest request) {

        User user = getUserByEmail(email);

        if (!passwordEncoder.matches(
                request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (passwordEncoder.matches(
                request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException(
                    "New password must differ from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getProfileImage(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
