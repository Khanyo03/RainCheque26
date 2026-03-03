package com.raincheque.controller;

import com.raincheque.dto.AuthRequest;
import com.raincheque.dto.AuthResponse;
import com.raincheque.entity.User;
import com.raincheque.security.JwtService;
import com.raincheque.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final UserService userService;
    private final JwtService jwtService;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
        try {
            log.info("Registration attempt for email: {}", request.getEmail());
            
            User user = userService.registerUser(
                    request.getEmail(),
                    request.getPassword(),
                    request.getFullName()
            );
            
            String token = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            
            AuthResponse response = AuthResponse.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole().name())
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        try {
            log.info("Login attempt for email: {}", request.getEmail());
            
            User user = userService.authenticateUser(request.getEmail(), request.getPassword());
            
            String token = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            
            AuthResponse response = AuthResponse.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole().name())
                    .build();
            
            log.info("Login successful for: {}", request.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Login failed for {}: {}", request.getEmail(), e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
}