package com.example.coffee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class CoffeeApplication {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(CoffeeApplication.class, args);
    }
}
