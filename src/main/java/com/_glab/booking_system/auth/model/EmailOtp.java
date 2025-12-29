package com._glab.booking_system.auth.model;

import com._glab.booking_system.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity for storing email OTP codes used as MFA fallback.
 * Codes are stored hashed and have a short expiry (10 minutes).
 */
@Entity
@Table(name = "email_otp", indexes = {
        @Index(name = "idx_email_otp_user", columnList = "user_id"),
        @Index(name = "idx_email_otp_hash", columnList = "codeHash")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class EmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * SHA-256 hash of the 6-digit OTP code.
     */
    @Column(nullable = false, length = 64)
    private String codeHash;

    /**
     * When this OTP expires (typically 10 minutes from creation).
     */
    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    /**
     * When this OTP was used (null if not yet used).
     */
    @Column
    private OffsetDateTime usedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Check if this OTP is still valid (not used and not expired).
     */
    public boolean isValid() {
        OffsetDateTime now = OffsetDateTime.now();
        return usedAt == null && now.isBefore(expiresAt);
    }

    /**
     * Mark this OTP as used.
     */
    public void markUsed() {
        this.usedAt = OffsetDateTime.now();
    }
}

