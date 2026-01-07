package com.geofleet.alert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka
@EnableScheduling
public class AlertProcessingApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlertProcessingApplication.class, args);
    }
}
