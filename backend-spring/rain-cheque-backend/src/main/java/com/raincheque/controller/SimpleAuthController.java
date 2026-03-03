package com.raincheque.controller;

import com.raincheque.dto.AuthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/simple-auth")
@CrossOrigin(origins = "http://localhost:5173")
public class SimpleAuthController {

    @PostMapping("/login")
    public ResponseEntity<?> simpleLogin(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");
        
        // Simple hardcoded check for demo
        if ("user@example.com".equals(email) && "Password123!".equals(password)) {
            return ResponseEntity.ok(AuthResponse.builder()
                    .token("dummy-token-for-testing")
                    .refreshToken("dummy-refresh-token")
                    .email(email)
                    .fullName("John Doe")
                    .role("USER")
                    .build());
        }
        
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid credentials"));
    }
}