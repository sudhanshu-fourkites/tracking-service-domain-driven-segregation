package com.fourkites.shipment.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;

/**
 * Configuration for Java 21 Virtual Threads
 * Enables virtual threads for web requests and async operations
 */
@Configuration
@EnableAsync
@ConditionalOnProperty(value = "spring.threads.virtual.enabled", havingValue = "true", matchIfMissing = true)
public class VirtualThreadConfig implements AsyncConfigurer {
    
    /**
     * Configure Tomcat to use virtual threads for handling HTTP requests
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadCustomizer() {
        return protocolHandler -> protocolHandler.setExecutor(
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    /**
     * Configure async task executor to use virtual threads
     */
    @Override
    @Bean(name = "taskExecutor")
    public AsyncTaskExecutor getAsyncExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Virtual thread executor for custom use cases
     */
    @Bean(name = "virtualThreadExecutor")
    public AsyncTaskExecutor virtualThreadExecutor() {
        var executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
            .name("virtual-", 0)
            .factory());
        return new TaskExecutorAdapter(executor);
    }
}