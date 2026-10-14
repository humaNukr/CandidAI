package ua.edu.ukma.candidai.common.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import ua.edu.ukma.candidai.common.util.CommonGenerator;

@Service
public class JwtService {

    private static final int MIN_KEY_LENGTH_BYTES = 32;
    private static final long SECONDS_IN_MINUTE = 60L;

    private final SecretKey signingKey;
    private final long expirationMinutes;
    private final CommonGenerator commonGenerator;

    @Autowired
    public JwtService(
            @Value("${candidai.security.jwt.secret}") String secret,
            @Value("${candidai.security.jwt.expiration-minutes:60}") long expirationMinutes,
            CommonGenerator commonGenerator) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "JWT секретний ключ повинен мати довжину щонайменше 256 біт (32 байти)"
            );
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMinutes = expirationMinutes;
        this.commonGenerator = commonGenerator != null ? commonGenerator : new CommonGenerator();
    }

    public JwtService(String secret, long expirationMinutes) {
        this(secret, expirationMinutes, new CommonGenerator());
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, expirationMinutes, ChronoUnit.MINUTES);
    }

    public String generateToken(UserDetails userDetails, long amount, ChronoUnit unit) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Instant now = commonGenerator.now();
        Instant expiry = now.plus(amount, unit);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationMinutes * SECONDS_IN_MINUTE;
    }
}
