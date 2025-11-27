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

@Entity
@Table(name = "refresh_token", indexes = {
        @Index(name = "idx_refresh_token_jti", columnList = "tokenId", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * JWT ID (jti) from the refresh token.
     */
    @Column(nullable = false, unique = true, length = 64)
    private String tokenId;

    /**
     * When this refresh token expires.
     */
    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    /**
     * When this refresh token was revoked (e.g., on rotation or logout).
     */
    @Column
    private OffsetDateTime revokedAt;

    /**
     * The tokenId of the new refresh token that replaced this one (for rotation tracking).
     */
    @Column(length = 64)
    private String replacedByTokenId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    public boolean isActive() {
        OffsetDateTime now = OffsetDateTime.now();
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public void revoke(String replacementTokenId) {
        this.revokedAt = OffsetDateTime.now();
        this.replacedByTokenId = replacementTokenId;
    }
}


