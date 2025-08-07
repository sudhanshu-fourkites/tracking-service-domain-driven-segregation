package com.fourkites.common.resilience;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resilience configuration with rate limiting and circuit breakers
 * Implements various resilience patterns for microservices
 */
@Configuration
public class RateLimitingConfig {
    
    /**
     * Circuit breaker configuration
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(3)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(IOException.class, RuntimeException.class)
            .build();
        
        return CircuitBreakerRegistry.of(config);
    }
    
    /**
     * Rate limiter configuration
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(100)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofSeconds(5))
            .build();
        
        return RateLimiterRegistry.of(config);
    }
    
    /**
     * Retry configuration
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .retryExceptions(IOException.class)
            .ignoreExceptions(IllegalArgumentException.class)
            .build();
        
        return RetryRegistry.of(config);
    }
    
    /**
     * Named circuit breakers for different services
     */
    @Bean
    public CircuitBreaker shipmentServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("shipment-service", CircuitBreakerConfig.custom()
            .failureRateThreshold(30)
            .slowCallRateThreshold(50)
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .build());
    }
    
    @Bean
    public CircuitBreaker locationServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("location-service", CircuitBreakerConfig.custom()
            .failureRateThreshold(40)
            .slowCallRateThreshold(60)
            .slowCallDurationThreshold(Duration.ofSeconds(1))
            .build());
    }
}

/**
 * Rate limiting filter using Bucket4j
 */
@Component
@Order(1)
class RateLimitingFilter implements Filter {
    
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Bandwidth bandwidth;
    
    public RateLimitingFilter() {
        // 100 requests per minute with refill
        this.bandwidth = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String clientId = getClientIdentifier(httpRequest);
        Bucket bucket = buckets.computeIfAbsent(clientId, k -> Bucket.builder()
            .addLimit(bandwidth)
            .build());
        
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            httpResponse.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            httpResponse.setHeader("Retry-After", "60");
            httpResponse.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
        }
    }
    
    private String getClientIdentifier(HttpServletRequest request) {
        // Use API key if present, otherwise use IP address
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null) {
            return apiKey;
        }
        
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null) {
            clientIp = request.getRemoteAddr();
        }
        return clientIp;
    }
}