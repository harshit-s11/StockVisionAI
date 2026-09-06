package com.stockvision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockVisionApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockVisionApplication.class, args);
    }
}