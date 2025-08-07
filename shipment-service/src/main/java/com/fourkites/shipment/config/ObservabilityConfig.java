package com.fourkites.shipment.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration for enhanced observability using Micrometer Observation API
 * Provides metrics, tracing, and logging correlation
 */
@Configuration
@EnableAspectJAutoProxy
public class ObservabilityConfig {
    
    /**
     * Enable @Observed annotation support
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
    
    /**
     * Customize observation registry with handlers
     */
    @Bean
    public ObservationRegistryCustomizer<ObservationRegistry> observationRegistryCustomizer(
            MeterRegistry meterRegistry, Tracer tracer) {
        return registry -> {
            registry.observationConfig()
                .observationHandler(new DefaultMeterObservationHandler(meterRegistry))
                .observationHandler(new DefaultTracingObservationHandler(tracer))
                .observationHandler(new LoggingObservationHandler());
        };
    }
    
    /**
     * Custom observation handler for structured logging
     */
    private static class LoggingObservationHandler implements io.micrometer.observation.ObservationHandler<io.micrometer.observation.Observation.Context> {
        
        @Override
        public void onStart(io.micrometer.observation.Observation.Context context) {
            context.put("startTime", System.currentTimeMillis());
        }
        
        @Override
        public void onStop(io.micrometer.observation.Observation.Context context) {
            Long startTime = context.get("startTime");
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                context.put("duration", duration);
            }
        }
        
        @Override
        public boolean supportsContext(io.micrometer.observation.Observation.Context context) {
            return true;
        }
    }
}