package com._glab.booking_system.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Application-wide configuration properties.
 * Configure via application.yml or environment variables.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private Mail mail = new Mail();
    private Frontend frontend = new Frontend();
    private Cors cors = new Cors();
    private Security security = new Security();
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Mail {
        /**
         * The "from" email address for outgoing emails.
         */
        private String from = "noreply@example.com";
    }

    @Getter
    @Setter
    public static class Frontend {
        /**
         * The frontend application URL (for building links in emails).
         */
        private String url = "http://localhost:3000";
    }

    @Getter
    @Setter
    public static class Cors {
        /**
         * Allowed origins for CORS requests.
         * Use "*" to allow all origins (not recommended for production).
         * Example: ["https://example.com", "https://app.example.com"]
         */
        private List<String> allowedOrigins = List.of("http://localhost:3000");

        /**
         * Allowed HTTP methods for CORS requests.
         */
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

        /**
         * Allowed headers for CORS requests.
         */
        private List<String> allowedHeaders = List.of("*");

        /**
         * Whether to allow credentials (cookies, authorization headers).
         */
        private boolean allowCredentials = true;

        /**
         * Max age in seconds for preflight cache.
         */
        private long maxAge = 3600;
    }

    @Getter
    @Setter
    public static class Security {
        /**
         * Whether CSRF protection is enabled.
         * Should be enabled when using cookie-based authentication with browser clients.
         * Can be disabled for pure JWT API usage.
         */
        private boolean csrfEnabled = false;
    }

    @Getter
    @Setter
    public static class RateLimit {
        /**
         * Whether rate limiting is enabled.
         */
        private boolean enabled = true;

        /**
         * Maximum requests per time window for authentication endpoints.
         */
        private int authRequestsPerMinute = 10;

        /**
         * Maximum requests per time window for public endpoints.
         */
        private int publicRequestsPerMinute = 60;

        /**
         * Maximum requests per time window for authenticated users.
         */
        private int authenticatedRequestsPerMinute = 120;
    }
}
