package com.fulfillment.warehouseservice.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.fulfillment.warehouseservice")
public class WarehouseFunctionalDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(WarehouseFunctionalDemoApplication.class, args);
    }
}
