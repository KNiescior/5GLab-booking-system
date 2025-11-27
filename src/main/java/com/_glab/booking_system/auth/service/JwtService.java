package com._glab.booking_system.auth.service;

import com._glab.booking_system.auth.config.JwtKeyProvider;
import com._glab.booking_system.auth.config.JwtProperties;
import com._glab.booking_system.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

	private final JwtKeyProvider keyProvider;
	private final JwtProperties jwtProperties;

	/**
	 * Generates an access token for the given user.
	 * Contains: sub (email), role, iat, exp, iss
	 */
	public String generateAccessToken(User user) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpiry().toMillis());

		return Jwts.builder()
				.subject(user.getEmail())
				.claim("role", user.getRole().getName().name())
				.claim("userId", user.getId())
				.issuedAt(now)
				.expiration(expiry)
				.issuer(jwtProperties.getIssuer())
				.signWith(keyProvider.getPrivateKey())
				.compact();
	}

	/**
	 * Generates a refresh token for the given user.
	 * Contains: sub (email), jti (unique ID for rotation tracking), iat, exp, iss
	 * Returns a record with the token and its jti.
	 */
	public RefreshTokenResult generateRefreshToken(User user) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiry().toMillis());
		String jti = UUID.randomUUID().toString();

		String token = Jwts.builder()
				.subject(user.getEmail())
				.id(jti)
				.claim("userId", user.getId())
				.issuedAt(now)
				.expiration(expiry)
				.issuer(jwtProperties.getIssuer())
				.signWith(keyProvider.getPrivateKey())
				.compact();

		return new RefreshTokenResult(token, jti, expiry);
	}

	/**
	 * Validates a token and returns its claims if valid.
	 */
	public Optional<Claims> validateToken(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(keyProvider.getPublicKey())
					.requireIssuer(jwtProperties.getIssuer())
					.build()
					.parseSignedClaims(token)
					.getPayload();
			return Optional.of(claims);
		} catch (JwtException e) {
			log.debug("Token validation failed: {}", e.getMessage());
			return Optional.empty();
		}
	}

	/**
	 * Extracts email (subject) from a valid token.
	 */
	public Optional<String> extractEmail(String token) {
		return validateToken(token).map(Claims::getSubject);
	}

	/**
	 * Extracts token ID (jti) from a valid token.
	 */
	public Optional<String> extractTokenId(String token) {
		return validateToken(token).map(Claims::getId);
	}

	/**
	 * Extracts user ID from a valid token.
	 */
	public Optional<Integer> extractUserId(String token) {
		return validateToken(token).map(claims -> claims.get("userId", Integer.class));
	}

	/**
	 * Extracts role from a valid token.
	 */
	public Optional<String> extractRole(String token) {
		return validateToken(token).map(claims -> claims.get("role", String.class));
	}

	/**
	 * Result of refresh token generation.
	 */
	public record RefreshTokenResult(String token, String jti, Date expiresAt) {}
}

