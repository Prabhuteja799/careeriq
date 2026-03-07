package com.careeriq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync        // Enables @Async — all AI pipeline calls run async
@EnableScheduling   // Enables @Scheduled — delta sync cron job
public class CareerIQApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareerIQApplication.class, args);
    }
}
