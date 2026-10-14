package ua.edu.ukma.candidai.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import ua.edu.ukma.candidai.common.util.CommonGenerator;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtValidatorService {

    private final SecretKey signingKey;
    private final CommonGenerator commonGenerator;

    @Autowired
    public JwtValidatorService(
            @Value("${candidai.security.jwt.secret}") String secret,
            CommonGenerator commonGenerator) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.commonGenerator = commonGenerator != null ? commonGenerator : new CommonGenerator();
    }

    public JwtValidatorService(String secret) {
        this(secret, new CommonGenerator());
    }

    public Claims extractAllClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT токен не може бути порожнім");
        }
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token.trim())
                .getPayload();
    }

    public String extractUsername(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        if (token == null || token.isBlank() || userDetails == null || userDetails.getUsername() == null) {
            return false;
        }
        try {
            Claims claims = extractAllClaims(token);
            return userDetails.getUsername().equalsIgnoreCase(claims.getSubject())
                    && isNotExpired(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isNotExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.toInstant().isAfter(commonGenerator.now());
    }
}

