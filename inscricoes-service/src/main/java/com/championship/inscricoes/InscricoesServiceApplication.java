package com.championship.inscricoes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InscricoesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InscricoesServiceApplication.class, args);
    }
}
