package com._glab.booking_system.auth.repository;

import com._glab.booking_system.auth.model.PasswordSetupToken;
import com._glab.booking_system.auth.model.TokenPurpose;
import com._glab.booking_system.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordSetupTokenRepository extends JpaRepository<PasswordSetupToken, UUID> {

	/**
	 * Find token by its hash (used during verification).
	 */
	Optional<PasswordSetupToken> findByTokenHash(String tokenHash);

	/**
	 * Find all valid (unused, not expired) tokens for a user and purpose.
	 */
	@Query("SELECT t FROM PasswordSetupToken t WHERE t.user = :user AND t.purpose = :purpose AND t.usedAt IS NULL AND t.expiresAt > :now")
	List<PasswordSetupToken> findValidTokensByUserAndPurpose(User user, TokenPurpose purpose, OffsetDateTime now);

	/**
	 * Invalidate all pending tokens for a user and purpose (e.g., when generating a new one).
	 */
	@Modifying
	@Query("UPDATE PasswordSetupToken t SET t.usedAt = :now WHERE t.user = :user AND t.purpose = :purpose AND t.usedAt IS NULL")
	int invalidateTokensForUser(User user, TokenPurpose purpose, OffsetDateTime now);

	/**
	 * Delete expired tokens (cleanup job).
	 */
	@Modifying
	@Query("DELETE FROM PasswordSetupToken t WHERE t.expiresAt < :cutoff")
	int deleteExpiredTokens(OffsetDateTime cutoff);
}

