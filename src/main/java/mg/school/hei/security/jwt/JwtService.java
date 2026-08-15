package mg.school.hei.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import mg.school.hei.model.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {
  private final SecretKey signingKey;
  private static final long EXPIRATION_MS = 1000L * 60 * 60 * 24;

  public JwtService(@Value("${JWT_SECRET}") String secret) {
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
  }

  public String generateToken(UUID userId, UserRole role) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(userId.toString())
        .claim("role", role.name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusMillis(EXPIRATION_MS)))
        .signWith(signingKey)
        .compact();
  }

  public Claims parseToken(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }
}
