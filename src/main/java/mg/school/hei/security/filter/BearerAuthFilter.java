package mg.school.hei.security.filter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.model.UserRole;
import mg.school.hei.security.jwt.JwtService;
import mg.school.hei.security.model.Principal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class BearerAuthFilter extends OncePerRequestFilter {
  private static final String ACCESS_TOKEN_QUERY_PARAM = "access_token";

  private final JwtService jwtService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String token = resolveToken(request);

    if (token != null) {
      try {
        var claims = jwtService.parseToken(token);
        UUID userId = UUID.fromString(claims.getSubject());
        UserRole role = UserRole.valueOf(claims.get("role", String.class));
        Principal principal = Principal.builder().userId(userId).role(role).build();

        var authentication =
            new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (JwtException | IllegalArgumentException e) {
        SecurityContextHolder.clearContext();
      }
    }

    chain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      return header.substring(7);
    }
    String queryToken = request.getParameter(ACCESS_TOKEN_QUERY_PARAM);
    return (queryToken != null && !queryToken.isBlank()) ? queryToken : null;
  }
}
