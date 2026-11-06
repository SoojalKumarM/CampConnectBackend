package com.campusconnect.security;

import com.campusconnect.config.AppProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long ttlMs;

    public JwtService(AppProperties props) {
        this.key = Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.ttlMs = props.getJwt().getExpirationHours() * 3_600_000L;
    }

    public String generate(Long userId) {
        Date now = new Date();
        return Jwts.builder().subject(String.valueOf(userId)).issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs)).signWith(key).compact();
    }

    public Long parseUserId(String token) {
        return Long.valueOf(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject());
    }
}
