package com.championship.ranking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RankingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RankingServiceApplication.class, args);
    }
}
