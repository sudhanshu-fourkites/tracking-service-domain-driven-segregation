package com.fourkites.shipment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.context.annotation.Bean;

@Configuration
public class PaginationConfig {
    
    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setMaxPageSize(100);  // Maximum 100 items per page
            resolver.setOneIndexedParameters(false);  // Page numbers start at 0
            resolver.setFallbackPageable(org.springframework.data.domain.PageRequest.of(0, 20));
        };
    }
}