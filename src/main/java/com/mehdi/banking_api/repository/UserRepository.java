package com.mehdi.banking_api.repository;

import com.mehdi.banking_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByVerificationToken(String token);

    @Modifying
    @Query("DELETE FROM User u WHERE u.verified = false AND u.tokenExpiresAt < :now")
    void deleteExpiredUnverifiedUsers(@Param("now") LocalDateTime now);
}