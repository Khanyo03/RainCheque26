package com.raincheque.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SecretsValidationConfig {

    private final Environment environment;

    public SecretsValidationConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateRequiredSecrets() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());

        // Skip validation in dev and test profiles
        if (activeProfiles.contains("dev") || activeProfiles.contains("test")) {
            return;
        }

        // Also skip if running in a test context (application name contains "test")
        String appName = environment.getProperty("spring.application.name", "");
        if (appName.toLowerCase().contains("test")) {
            return;
        }

        List<String> requiredVars = List.of(
                "SPRING_SECURITY_JWT_SECRET",
                "APP_QR_SECRET"
        );

        List<String> missingVars = new ArrayList<>();
        for (String var : requiredVars) {
            String value = System.getenv(var);
            if (value == null || value.isBlank()) {
                missingVars.add(var);
            }
        }

        if (!missingVars.isEmpty()) {
            throw new IllegalStateException(
                    "Required environment variables are not set: " + String.join(", ", missingVars)
            );
        }
    }
}
