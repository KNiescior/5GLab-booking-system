package com._glab.booking_system.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtKeyProvider {

	private final JwtProperties jwtProperties;
	private final ResourceLoader resourceLoader;

	private PrivateKey privateKey;
	private PublicKey publicKey;

	@PostConstruct
	public void init() {
		try {
			this.privateKey = loadPrivateKey();
			this.publicKey = loadPublicKey();
			log.info("JWT keys loaded successfully");
		} catch (Exception e) {
			log.error("Failed to load JWT keys", e);
			throw new IllegalStateException("Failed to load JWT keys", e);
		}
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	private PrivateKey loadPrivateKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		String keyContent = loadKeyContent(jwtProperties.getPrivateKeyPath());
		
		// Handle both PKCS#8 and PKCS#1 formats
		String privateKeyPEM = keyContent
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replace("-----BEGIN RSA PRIVATE KEY-----", "")
				.replace("-----END RSA PRIVATE KEY-----", "")
				.replaceAll("\\s", "");

		byte[] keyBytes = Base64.getDecoder().decode(privateKeyPEM);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		return keyFactory.generatePrivate(keySpec);
	}

	private PublicKey loadPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		String keyContent = loadKeyContent(jwtProperties.getPublicKeyPath());
		
		String publicKeyPEM = keyContent
				.replace("-----BEGIN PUBLIC KEY-----", "")
				.replace("-----END PUBLIC KEY-----", "")
				.replaceAll("\\s", "");

		byte[] keyBytes = Base64.getDecoder().decode(publicKeyPEM);
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		return keyFactory.generatePublic(keySpec);
	}

	private String loadKeyContent(String path) throws IOException {
		try (InputStream is = resourceLoader.getResource(path).getInputStream()) {
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}

