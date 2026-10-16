
package com.lostfound.repository;

import com.lostfound.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
        select u
        from User u
        where lower(u.email) = lower(:identifier)
           or u.phone = :identifier
        """)
    Optional<User> findByEmail(
            @Param("identifier") String identifier);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByPhone(String phone);

    /**
     * Looks up an account using email or a mobile number.
     * Mobile numbers are tried in canonical +91 format and
     * common legacy formats to avoid breaking older accounts.
     */
    default Optional<User> findByLoginIdentifier(String identifier) {

        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }

        String value = identifier.trim();

        if (value.contains("@")) {
            return findByEmailIgnoreCase(value);
        }

        String digits = value.replaceAll("\\D", "");

        if (digits.length() == 10) {
            String canonical = "+91" + digits;

            Optional<User> result = findByPhone(canonical);
            if (result.isPresent()) {
                return result;
            }
        }

        if (digits.length() == 12 && digits.startsWith("91")) {
            String canonical = "+" + digits;

            Optional<User> result = findByPhone(canonical);
            if (result.isPresent()) {
                return result;
            }
        }

        Optional<User> result = findByPhone(value);
        if (result.isPresent()) {
            return result;
        }

        if (!digits.isBlank()) {
            result = findByPhone(digits);
            if (result.isPresent()) {
                return result;
            }
        }

        return findByEmail(value);
    }

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);
}
