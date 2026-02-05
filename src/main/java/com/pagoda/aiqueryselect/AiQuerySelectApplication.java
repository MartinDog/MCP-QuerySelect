package com.pagoda.aiqueryselect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AiQuerySelectApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiQuerySelectApplication.class, args);
    }

}
