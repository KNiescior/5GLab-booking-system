package com._glab.booking_system.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Application-wide configuration properties.
 */

//TODO: move to.env
@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private Mail mail = new Mail();
    private Frontend frontend = new Frontend();

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
}
