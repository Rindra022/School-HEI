package mg.school.hei.security.jwt;

import static org.assertj.core.api.Assertions.*;

import io.jsonwebtoken.JwtException;
import java.util.UUID;
import mg.school.hei.model.UserRole;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private final JwtService jwtService =
      new JwtService("test-secret-key-only-for-unit-tests-32-chars-min");

  @Test
  void generateToken_should_embed_the_user_id_and_role() {
    UUID userId = UUID.randomUUID();
    var token = jwtService.generateToken(userId, UserRole.TEACHER);

    var claims = jwtService.parseToken(token);

    assertThat(claims.getSubject()).isEqualTo(userId.toString());
    assertThat(claims.get("role", String.class)).isEqualTo("TEACHER");
  }

  @Test
  void parseToken_should_throw_for_a_malformed_token() {
    assertThatThrownBy(() -> jwtService.parseToken("not-a-real-token"))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void parseToken_should_throw_for_a_token_signed_with_a_different_secret() {
    var otherService = new JwtService("a-completely-different-secret-key-32-chars-min");
    var token = otherService.generateToken(UUID.randomUUID(), UserRole.STUDENT);

    assertThatThrownBy(() -> jwtService.parseToken(token)).isInstanceOf(JwtException.class);
  }
}
