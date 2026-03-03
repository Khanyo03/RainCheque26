package com.raincheque.controller;

import com.raincheque.entity.User;
import com.raincheque.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class TestController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/check-login")
    public ResponseEntity<?> checkLogin(@RequestBody LoginCheckRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        
        if (user == null) {
            response.put("exists", false);
            response.put("message", "User not found");
            return ResponseEntity.ok(response);
        }
        
        // Check password
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        
        response.put("exists", true);
        response.put("passwordMatches", passwordMatches);
        response.put("email", user.getEmail());
        response.put("userId", user.getId());
        response.put("storedHash", user.getPasswordHash());
        response.put("providedPassword", request.getPassword());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-test-user")
    public ResponseEntity<?> createTestUser() {
        try {
            // Check if user already exists
            if (userRepository.findByEmail("test@example.com").isPresent()) {
                return ResponseEntity.ok(Map.of("message", "Test user already exists"));
            }

            // Create new test user
            User user = new User();
            user.setEmail("test@example.com");
            user.setPasswordHash(passwordEncoder.encode("test123"));
            user.setFullName("Test User");
            user.setRole(User.Role.USER);
            user.setEmailVerified(true);
            
            userRepository.save(user);
            
            return ResponseEntity.ok(Map.of(
                "message", "Test user created successfully",
                "email", "test@example.com",
                "password", "test123"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public static class LoginCheckRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}