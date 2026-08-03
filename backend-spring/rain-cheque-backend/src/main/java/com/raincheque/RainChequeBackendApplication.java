package com.raincheque;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(
    exclude = {
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
    }
)
@EnableJpaAuditing
@EnableAsync
public class RainChequeBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(RainChequeBackendApplication.class, args);
    }
}
