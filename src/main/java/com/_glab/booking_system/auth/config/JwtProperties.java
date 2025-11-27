package com._glab.booking_system.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

	private String privateKeyPath = "classpath:keys/private.pem";
	private String publicKeyPath = "classpath:keys/public.pem";
	private Duration accessTokenExpiry = Duration.ofMinutes(15);
	private Duration refreshTokenExpiry = Duration.ofDays(7);
	private String issuer = "booking-system";
}

