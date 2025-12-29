package com._glab.booking_system.auth.repository;

import com._glab.booking_system.auth.model.EmailOtp;
import com._glab.booking_system.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, UUID> {

    /**
     * Find a valid (unused, not expired) OTP for the given user and code hash.
     */
    @Query("SELECT o FROM EmailOtp o WHERE o.user = :user AND o.codeHash = :codeHash AND o.usedAt IS NULL AND o.expiresAt > :now")
    Optional<EmailOtp> findValidOtp(User user, String codeHash, OffsetDateTime now);

    /**
     * Find the most recent OTP for a user (for rate limiting checks).
     */
    Optional<EmailOtp> findTopByUserOrderByCreatedAtDesc(User user);

    /**
     * Delete expired OTPs (cleanup job).
     */
    @Modifying
    @Query("DELETE FROM EmailOtp o WHERE o.expiresAt < :cutoff")
    int deleteExpiredOtps(OffsetDateTime cutoff);

    /**
     * Invalidate all pending OTPs for a user (e.g., after successful login).
     */
    @Modifying
    @Query("UPDATE EmailOtp o SET o.usedAt = :now WHERE o.user = :user AND o.usedAt IS NULL")
    int invalidateAllForUser(User user, OffsetDateTime now);
}

