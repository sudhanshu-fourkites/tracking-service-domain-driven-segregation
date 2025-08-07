package com.fourkites.common.exception;

import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.spring.web.advice.ProblemHandling;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler implementing RFC 7807 Problem Details
 * Provides consistent error responses across all microservices
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler implements ProblemHandling {
    
    private static final String INSTANCE_PREFIX = "/errors/";
    
    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Problem> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        Map<String, String> violations = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                (existing, replacement) -> existing
            ));
        
        Problem problem = Problem.builder()
            .withType(URI.create(INSTANCE_PREFIX + "validation-error"))
            .withTitle("Validation Error")
            .withStatus(Status.BAD_REQUEST)
            .withDetail("Request validation failed")
            .withInstance(URI.create(request.getRequestURI()))
            .with("timestamp", Instant.now())
            .with("violations", violations)
            .build();
        
        log.warn("Validation error: {}", violations);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }
    
    /**
     * Handle constraint violations
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Problem> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        
        Map<String, String> violations = ex.getConstraintViolations()
            .stream()
            .collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(),
                ConstraintViolation::getMessage
            ));
        
        Problem problem = Problem.builder()
            .withType(URI.create(INSTANCE_PREFIX + "constraint-violation"))
            .withTitle("Constraint Violation")
            .withStatus(Status.BAD_REQUEST)
            .withDetail("Constraint validation failed")
            .withInstance(URI.create(request.getRequestURI()))
            .with("timestamp", Instant.now())
            .with("violations", violations)
            .build();
        
        log.warn("Constraint violation: {}", violations);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }
    
    /**
     * Handle resource not found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Problem> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        Problem problem = Problem.builder()
            .withType(URI.create(INSTANCE_PREFIX + "resource-not-found"))
            .withTitle("Resource Not Found")
            .withStatus(Status.NOT_FOUND)
            .withDetail(ex.getMessage())
            .withInstance(URI.create(request.getRequestURI()))
            .with("timestamp", Instant.now())
            .with("resource", ex.getResourceType())
            .with("identifier", ex.getResourceId())
            .build();
        
        log.info("Resource not found: {} with id {}", ex.getResourceType(), ex.getResourceId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }
    
    /**
     * Handle business rule violations
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Problem> handleBusinessRuleViolation(
            BusinessRuleException ex,
            HttpServletRequest request) {
        
        Problem problem = Problem.builder()
            .withType(URI.create(INSTANCE_PREFIX + "business-rule-violation"))
            .withTitle("Business Rule Violation")
            .withStatus(Status.UNPROCESSABLE_ENTITY)
            .withDetail(ex.getMessage())
            .withInstance(URI.create(request.getRequestURI()))
            .with("timestamp", Instant.now())
            .with("rule", ex.getRuleName())
            .with("context", ex.getContext())
            .build();
        
        log.warn("Business rule violation: {} - {}", ex.getRuleName(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }
    
    /**
     * Handle rate limiting
     */
    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<Problem> handleRateLimitExceeded(
            RequestNotPermitted ex,
            HttpServletRequest request) {
        
        Problem problem = Problem.builder()
            .withType(URI.create(INSTANCE_PREFIX + "rate-limit-exceeded"))
            .withTitle("Rate Limit Exceeded")
            .withStatus(Status.TOO_MANY_REQUESTS)
            .withDetail("Too many requests. Please retry after some time.")
            .withInstance(URI.create(request.getRequestURI()))
            .with("timestamp", Instant.now())
            .build();
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", "60");
        
        log.warn("Rate limit exceeded for request: {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .headers(headers)
            .body(problem);
    }
    
    /**
     * Handle circuit breaker open
     */
    @ExceptionHandler(CircuitBreakerOpenException.class)
    public ResponseEntity<Problem> handleCircuitBreakerOpen(
            CircuitBreakerOpenException ex,
            HttpServletRequest request) {
        
        Problem problem = Problem.builder()
            .withType(URI.create(INSTANCE_PREFIX + "service-unavailable"))
            .withTitle("Service Temporarily Unavailable")
            .withStatus(Status.SERVICE_UNAVAILABLE)
            .withDetail("Service is temporarily unavailable. Please try again later.")
            .withInstance(URI.create(request.getRequestURI()))
            .with("timestamp", Instant.now())
            .with("circuit", ex.getCausingCircuitBreakerName())
            .build();
        
        log.error("Circuit breaker open: {}", ex.getCausingCircuitBreakerName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }
    
    /**
     * Handle concurrent modification
     */
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<Problem> handleOptimisticLock(
            OptimisticLockException ex,
            HttpServletRequest request) {
        
        Problem problem = Problem.builder()
            .withType(URI.create(INSTANCE_PREFIX + "concurrent-modification"))
            .withTitle("Concurrent Modification")
            .withStatus(Status.CONFLICT)
            .withDetail("Resource was modified by another request. Please retry.")
            .withInstance(URI.create(request.getRequestURI()))
            .with("timestamp", Instant.now())
            .build();
        
        log.info("Optimistic lock conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
    
    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Problem> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        Problem problem = Problem.builder()
            .withType(URI.create(INSTANCE_PREFIX + "internal-error"))
            .withTitle("Internal Server Error")
            .withStatus(Status.INTERNAL_SERVER_ERROR)
            .withDetail("An unexpected error occurred")
            .withInstance(URI.create(request.getRequestURI()))
            .with("timestamp", Instant.now())
            .build();
        
        log.error("Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}

// Custom exception classes

class ResourceNotFoundException extends RuntimeException {
    private final String resourceType;
    private final String resourceId;
    
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super("Resource not found: " + resourceType + " with id " + resourceId);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public String getResourceId() {
        return resourceId;
    }
}

class BusinessRuleException extends RuntimeException {
    private final String ruleName;
    private final Map<String, Object> context;
    
    public BusinessRuleException(String ruleName, String message, Map<String, Object> context) {
        super(message);
        this.ruleName = ruleName;
        this.context = context;
    }
    
    public String getRuleName() {
        return ruleName;
    }
    
    public Map<String, Object> getContext() {
        return context;
    }
}

class OptimisticLockException extends RuntimeException {
    public OptimisticLockException(String message) {
        super(message);
    }
}