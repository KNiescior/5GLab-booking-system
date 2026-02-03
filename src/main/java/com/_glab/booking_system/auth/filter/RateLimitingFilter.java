package com._glab.booking_system.auth.filter;

import com._glab.booking_system.auth.config.AppProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter to protect against abuse.
 * Uses Bucket4j for token bucket algorithm implementation.
 * 
 * Rate limits are applied per IP address with different limits for:
 * - Authentication endpoints (stricter to prevent brute force)
 * - Public endpoints (moderate)
 * - Authenticated requests (more generous)
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final AppProperties appProperties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    // Auth endpoint paths that need stricter rate limiting
    private static final String[] AUTH_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/setup-password",
            "/api/v1/auth/mfa/verify",
            "/api/v1/auth/mfa/email-code"
    };

    public RateLimitingFilter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        if (!appProperties.getRateLimit().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        
        RateLimitType limitType = determineRateLimitType(path);
        String bucketKey = clientIp + ":" + limitType.name();
        
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(limitType));

        if (bucket.tryConsume(1)) {
            // Add rate limit headers
            response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on path: {} (type: {})", clientIp, path, limitType);
            sendRateLimitExceededResponse(response, limitType);
        }
    }

    private RateLimitType determineRateLimitType(String path) {
        // Check if it's an auth endpoint
        for (String authPath : AUTH_PATHS) {
            if (path.startsWith(authPath)) {
                return RateLimitType.AUTH;
            }
        }
        
        // Check if user is authenticated (this runs after JWT filter in the chain)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return RateLimitType.AUTHENTICATED;
        }
        
        return RateLimitType.PUBLIC;
    }

    private Bucket createBucket(RateLimitType type) {
        int requestsPerMinute = switch (type) {
            case AUTH -> appProperties.getRateLimit().getAuthRequestsPerMinute();
            case PUBLIC -> appProperties.getRateLimit().getPublicRequestsPerMinute();
            case AUTHENTICATED -> appProperties.getRateLimit().getAuthenticatedRequestsPerMinute();
        };

        Bandwidth limit = Bandwidth.classic(requestsPerMinute, 
                Refill.greedy(requestsPerMinute, Duration.ofMinutes(1)));
        
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        // Check for forwarded IP (when behind proxy/load balancer)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in case of multiple proxies
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private void sendRateLimitExceededResponse(HttpServletResponse response, RateLimitType type) 
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");
        
        String message = switch (type) {
            case AUTH -> "Too many authentication attempts. Please try again later.";
            case PUBLIC -> "Too many requests. Please slow down.";
            case AUTHENTICATED -> "Rate limit exceeded. Please try again later.";
        };
        
        String jsonResponse = String.format(
                "{\"error\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"%s\",\"retryAfterSeconds\":60}",
                message
        );
        
        response.getWriter().write(jsonResponse);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Don't rate limit health checks, actuator, or swagger
        String path = request.getRequestURI();
        return path.startsWith("/actuator") 
                || path.startsWith("/v3/api-docs") 
                || path.startsWith("/swagger-ui");
    }

    private enum RateLimitType {
        AUTH,
        PUBLIC,
        AUTHENTICATED
    }
}
