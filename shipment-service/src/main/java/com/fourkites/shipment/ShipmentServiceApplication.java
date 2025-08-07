package com.fourkites.shipment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableJpaAuditing
public class ShipmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShipmentServiceApplication.class, args);
    }
}