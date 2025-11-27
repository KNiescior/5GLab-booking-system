package com._glab.booking_system.auth.service;

import com._glab.booking_system.auth.config.JwtKeyProvider;
import com._glab.booking_system.auth.config.JwtProperties;
import com._glab.booking_system.auth.exception.ExpiredJwtTokenException;
import com._glab.booking_system.auth.exception.InvalidJwtException;
import com._glab.booking_system.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
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
	 * Parses and validates a token. Throws domain-specific exceptions on failure.
	 */
	public Claims parseToken(String token) {
		try {
			return Jwts.parser()
					.verifyWith(keyProvider.getPublicKey())
					.requireIssuer(jwtProperties.getIssuer())
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch (ExpiredJwtException e) {
			log.debug("JWT token expired: {}", e.getMessage());
			throw new ExpiredJwtTokenException("JWT token expired", e);
		} catch (JwtException e) {
			log.debug("JWT token invalid: {}", e.getMessage());
			throw new InvalidJwtException("JWT token is invalid", e);
		}
	}

	/**
	 * Extracts email (subject) from a valid token.
	 */
	public String extractEmail(String token) {
		return parseToken(token).getSubject();
	}

	/**
	 * Extracts token ID (jti) from a valid token.
	 */
	public String extractTokenId(String token) {
		return parseToken(token).getId();
	}

	/**
	 * Extracts user ID from a valid token.
	 */
	public Integer extractUserId(String token) {
		return parseToken(token).get("userId", Integer.class);
	}

	/**
	 * Extracts role from a valid token.
	 */
	public String extractRole(String token) {
		return parseToken(token).get("role", String.class);
	}

	/**
	 * Result of refresh token generation.
	 */
	public record RefreshTokenResult(String token, String jti, Date expiresAt) {}
}

