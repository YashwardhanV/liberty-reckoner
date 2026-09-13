package in.gov.libertyreckoner.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {
    private final byte[] secret;
    private final long expirationMinutes;

    public JwtService(@Value("${libertyreckoner.jwt.secret}") String secret,
                      @Value("${libertyreckoner.jwt.expiration-minutes}") long expirationMinutes) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMinutes = expirationMinutes;
    }

    public String generate(UserDetails user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("roles", user.getAuthorities().stream().map(Object::toString).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(Keys.hmacShaKeyFor(secret))
                .compact();
    }

    public String username(String token) {
        return claims(token).getSubject();
    }

    public boolean valid(String token, UserDetails user) {
        Claims claims = claims(token);
        return claims.getSubject().equalsIgnoreCase(user.getUsername()) && claims.getExpiration().after(new Date());
    }

    public long expirationSeconds() {
        return expirationMinutes * 60;
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret)).build()
                .parseSignedClaims(token).getPayload();
    }
}

