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
@Table(name = "password_setup_token", indexes = {
	@Index(name = "idx_password_setup_token_hash", columnList = "tokenHash", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PasswordSetupToken {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/**
	 * SHA-256 hash of the raw token. The raw token is only sent via email.
	 */
	@Column(nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(nullable = false)
	private OffsetDateTime expiresAt;

	@Column
	private OffsetDateTime usedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private TokenPurpose purpose;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	/**
	 * Checks if the token is valid (not expired, not used).
	 */
	public boolean isValid() {
		return usedAt == null && OffsetDateTime.now().isBefore(expiresAt);
	}

	/**
	 * Marks the token as used.
	 */
	public void markUsed() {
		this.usedAt = OffsetDateTime.now();
	}
}
