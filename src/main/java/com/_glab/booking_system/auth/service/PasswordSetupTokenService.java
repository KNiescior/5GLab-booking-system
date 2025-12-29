package com._glab.booking_system.auth.service;

import com._glab.booking_system.auth.exception.ExpiredPasswordSetupTokenException;
import com._glab.booking_system.auth.exception.InvalidPasswordSetupTokenException;
import com._glab.booking_system.auth.model.PasswordSetupToken;
import com._glab.booking_system.auth.model.TokenPurpose;
import com._glab.booking_system.auth.repository.PasswordSetupTokenRepository;
import com._glab.booking_system.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PasswordSetupTokenService {

	private static final int TOKEN_BYTES = 32;
	private static final int EXPIRY_HOURS = 48;

	private final PasswordSetupTokenRepository tokenRepository;

	/**
	 * Creates a new token for the given user and purpose.
	 * Invalidates any existing tokens for the same user/purpose.
	 * Returns the raw token string (to be sent via email).
	 */
	@Transactional
	public String createToken(User user, TokenPurpose purpose) {
		// Invalidate existing tokens
		tokenRepository.invalidateTokensForUser(user, purpose, OffsetDateTime.now());

		// Generate new token
		String rawToken = generateSecureToken();
		String hash = hashToken(rawToken);

		PasswordSetupToken entity = new PasswordSetupToken();
		entity.setUser(user);
		entity.setTokenHash(hash);
		entity.setPurpose(purpose);
		entity.setExpiresAt(OffsetDateTime.now().plusHours(EXPIRY_HOURS));

		tokenRepository.save(entity);

		return rawToken;
	}

	/**
	 * Validates and consumes a token, throwing specific exceptions on failure.
	 * Returns the associated user on success.
	 */
	@Transactional
	public User validateAndConsumeToken(String rawToken) {
		String hash = hashToken(rawToken);
		PasswordSetupToken token = tokenRepository.findByTokenHash(hash)
				.orElseThrow(() -> new InvalidPasswordSetupTokenException("Password setup token not found"));

		if (token.getUsedAt() != null) {
			throw new InvalidPasswordSetupTokenException("Password setup token has already been used");
		}

		if (OffsetDateTime.now().isAfter(token.getExpiresAt())) {
			throw new ExpiredPasswordSetupTokenException("Password setup token has expired");
		}

		token.markUsed();
		tokenRepository.save(token);

		return token.getUser();
	}

	/**
	 * Deletes expired tokens (for scheduled cleanup).
	 */
	@Transactional
	public int cleanupExpiredTokens() {
		return tokenRepository.deleteExpiredTokens(OffsetDateTime.now());
	}

	private String generateSecureToken() {
		SecureRandom random = new SecureRandom();
		byte[] bytes = new byte[TOKEN_BYTES];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hashToken(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(rawToken.getBytes());
			StringBuilder hexString = new StringBuilder();
			for (byte b : hashBytes) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 not available", e);
		}
	}
}





