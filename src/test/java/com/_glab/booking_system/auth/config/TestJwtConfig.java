package com._glab.booking_system.auth.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

@TestConfiguration
public class TestJwtConfig {

    private static final KeyPair KEY_PAIR = generateKeyPair();

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate RSA key pair for tests", e);
        }
    }

    @Bean
    public JwtKeyProvider jwtKeyProvider() {
        return new JwtKeyProvider(null, null) {
            @Override
            public void init() {
                // Skip file loading - use generated keys
            }

            @Override
            public PrivateKey getPrivateKey() {
                return KEY_PAIR.getPrivate();
            }

            @Override
            public PublicKey getPublicKey() {
                return KEY_PAIR.getPublic();
            }
        };
    }
}
