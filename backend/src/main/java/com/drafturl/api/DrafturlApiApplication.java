package com.drafturl.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DrafturlApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DrafturlApiApplication.class, args);
    }
}
