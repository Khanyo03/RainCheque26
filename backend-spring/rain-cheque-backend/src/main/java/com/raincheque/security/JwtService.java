package com.raincheque.security;

import com.raincheque.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${spring.security.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;

    @Value("${spring.security.jwt.expiration:86400000}")
    private long jwtExpiration;

    @Value("${spring.security.jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    /**
     * Extract username from JWT token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generate token for user
     */
    public String generateToken(User user) {
        return generateToken(new HashMap<>(), user);
    }

    /**
     * Generate token with extra claims
     */
    public String generateToken(Map<String, Object> extraClaims, User user) {
        return buildToken(extraClaims, user, jwtExpiration);
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(User user) {
        return buildToken(new HashMap<>(), user, refreshExpiration);
    }

    /**
     * Build token with claims and expiration
     */
    private String buildToken(Map<String, Object> extraClaims, User user, long expiration) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate token
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Check if token is expired
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extract expiration date from token
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract all
     *  claims from token
     */
    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Get signing key from secret
     */
    private Key getSignInKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Validate token without UserDetails (basic validation)
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get user email from token
     */
    public String getUserEmailFromToken(String token) {
        return extractUsername(token);
    }

    /**
     * Get token expiration time in milliseconds
     */
    public long getExpirationTime() {
        return jwtExpiration;
    }

    /**
     * Check if token can be refreshed
     */
    public boolean canRefreshToken(String token) {
    try {
        Claims claims = extractAllClaims(token);

        // Check if token is expired
        Date expiration = claims.getExpiration();
        if (expiration != null && expiration.before(new Date())) {
            return false;
        }

        // Optional: check if token is revoked/blacklisted
        // e.g., query a DB or cache of invalidated tokens

        // Optional: enforce a refresh window (e.g., only refresh if issuedAt is older than X)
        Date issuedAt = claims.getIssuedAt();
        if (issuedAt != null) {
            long ageMillis = System.currentTimeMillis() - issuedAt.getTime();
            if (ageMillis < 60_000) { // example: don’t refresh if younger than 1 minute
                return false;
            }
        }

        return true;
    } catch (JwtException e) {
        // Invalid token (bad signature, malformed, etc.)
        return false;
    }
}


    /**
     * Refresh token
     */
    public String refreshToken(String token) {
    if (!canRefreshToken(token)) {
        throw new RuntimeException("Token cannot be refreshed");
    }

    Claims claims = extractAllClaims(token);

    // Create a new claims map to avoid mutating the original
    Map<String, Object> refreshedClaims = new HashMap<>(claims);
    Date now = new Date(System.currentTimeMillis());
    Date expiry = new Date(System.currentTimeMillis() + jwtExpiration);

    return Jwts.builder()
            .setClaims(refreshedClaims)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(getSignInKey(), SignatureAlgorithm.HS256)
            .compact();
}

}