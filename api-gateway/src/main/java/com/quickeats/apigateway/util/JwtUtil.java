package com.quickeats.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility to parse and validate JWT tokens at the API Gateway level.
 * Reads the shared secret key and verifies expiration and cryptographic signatures.
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parses the JWT and extracts all claims.
     * Throws exceptions if the token is expired or the signature is invalid.
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Checks whether the token has expired.
     */
    public boolean isExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    /**
     * Extract username (subject) from claims.
     */
    public String getUsername(Claims claims) {
        return claims.getSubject();
    }

    /**
     * Extract specific claim value.
     */
    public String getClaimValue(Claims claims, String key) {
        Object val = claims.get(key);
        return val != null ? val.toString() : null;
    }
}
