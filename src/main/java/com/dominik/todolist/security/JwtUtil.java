package com.dominik.todolist.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private final long expirationTimeMillis;

    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secretString,
                   @Value("${jwt.expiration.ms}") long expirationTimeMillis) {
        this.expirationTimeMillis = expirationTimeMillis;

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secretString);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid Base64 encoding for JWT secret: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid Base64 encoding for JWT secret", e);
        }

        if (keyBytes.length < 32) {
            logger.warn("JWT Secret key is less than 256 bits ({}) bytes. This is not recommended for HS256.",
                    keyBytes.length);
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        logger.info("JWT Secret Key initialized successfully.");
    }

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    /**
     * Generates a JWT for a UserDetails object (common in Spring Security).
     *
     * @param userDetails The UserDetails object.
     * @return The generated JWT string.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Instant now = Instant.now();
        Date expirationDate = Date.from(now.plusMillis(expirationTimeMillis));

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(expirationDate)
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from the JWT using a claims resolver function.
     *
     * @param token          The JWT string.
     * @param claimsResolver A function to extract the desired claim.
     * @param <T>            The type of the claim.
     * @return The extracted claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token has expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            logger.error("JWT token is malformed: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            logger.error("JWT signature validation failed: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty or invalid: {}", e.getMessage());
            throw e;
        }
    }

    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            logger.error("Could not determine token expiration: {}", e.getMessage());
            return true;
        }
    }

    public Boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername());
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Boolean isTokenStructureValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Token structure/signature/expiration validation failed: {}", e.getMessage());
            return false;
        }
    }
}
