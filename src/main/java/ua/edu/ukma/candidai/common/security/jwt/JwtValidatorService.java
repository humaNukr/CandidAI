package ua.edu.ukma.candidai.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtValidatorService {

    private final SecretKey signingKey;

    public JwtValidatorService(@Value("${candidai.security.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<Claims> parseTokenClaims(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();
            return Optional.of(claims);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public String extractUsername(String token) {
        return parseTokenClaims(token)
                .map(Claims::getSubject)
                .orElse(null);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            return false;
        }
        return parseTokenClaims(token)
                .filter(claims -> userDetails.getUsername().equalsIgnoreCase(claims.getSubject()))
                .filter(this::isNotExpired)
                .isPresent();
    }

    private boolean isNotExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.after(new Date());
    }
}
