package com.fourkites.shipment.validation;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class PaginationValidator {
    
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;
    
    public void validate(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                String.format("Page size must not exceed %d items. Requested: %d", 
                    MAX_PAGE_SIZE, pageable.getPageSize())
            );
        }
        
        if (pageable.getPageSize() <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }
        
        if (pageable.getPageNumber() < 0) {
            throw new IllegalArgumentException("Page number must not be negative");
        }
    }
    
    public Pageable sanitize(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            return org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), 
                MAX_PAGE_SIZE, 
                pageable.getSort()
            );
        }
        return pageable;
    }
}