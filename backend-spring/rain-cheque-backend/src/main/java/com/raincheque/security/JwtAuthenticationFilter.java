package com.raincheque.security;

import com.raincheque.entity.User;
import com.raincheque.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Skip filter for auth endpoints
        String path = request.getRequestURI();
        if (path.contains("/api/v1/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if Authorization header exists and starts with Bearer
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT token
        jwt = authHeader.substring(7);
        
        try {
            // Extract user email from token
            userEmail = jwtService.extractUsername(jwt);
            
            // If we have email and user is not already authenticated
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Load JPA User entity from database
                Optional<User> optionalUser = userRepository.findByEmail(userEmail);
                
                if (optionalUser.isEmpty()) {
                    // User not found for valid JWT - skip setting authentication
                    log.warn("JWT valid but user not found in database: {}", userEmail);
                    filterChain.doFilter(request, response);
                    return;
                }
                
                User user = optionalUser.get();
                
                // Build UserDetails for token validation (JwtService expects UserDetails)
                UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                        .username(user.getEmail())
                        .password(user.getPasswordHash())
                        .authorities("ROLE_" + user.getRole().name())
                        .build();
                
                // Validate token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Build authorities from user's role
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                    );
                    
                    // Create authentication token with JPA User entity as principal
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            authorities
                    );
                    
                    // Set details
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Set authentication in context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("User authenticated: {}", userEmail);
                }
            }
        } catch (Exception e) {
            log.error("JWT Authentication failed: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
}
