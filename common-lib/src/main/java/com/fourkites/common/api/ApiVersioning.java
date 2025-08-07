package com.fourkites.common.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.lang.annotation.*;

/**
 * API Versioning configuration using URL path versioning
 * Supports multiple API versions with backward compatibility
 */
@Configuration
public class ApiVersioning implements WebMvcConfigurer {
    
    private static final String VERSION_PREFIX = "/api/v";
    
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(VERSION_PREFIX + "1", 
            c -> c.isAnnotationPresent(RestController.class) && 
                 c.isAnnotationPresent(ApiV1.class));
        
        configurer.addPathPrefix(VERSION_PREFIX + "2", 
            c -> c.isAnnotationPresent(RestController.class) && 
                 c.isAnnotationPresent(ApiV2.class));
    }
}

/**
 * Marker annotation for API version 1
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface ApiV1 {
}

/**
 * Marker annotation for API version 2
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface ApiV2 {
}

/**
 * API version header constants
 */
class ApiVersionHeaders {
    public static final String API_VERSION = "X-API-Version";
    public static final String MIN_VERSION = "X-Min-Version";
    public static final String DEPRECATED = "X-Deprecated";
    public static final String SUNSET = "X-Sunset-Date";
}