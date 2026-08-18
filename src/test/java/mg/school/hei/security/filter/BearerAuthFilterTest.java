package mg.school.hei.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import mg.school.hei.model.UserRole;
import mg.school.hei.security.jwt.JwtService;
import mg.school.hei.security.model.Principal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class BearerAuthFilterTest {

  private final JwtService jwtService =
      new JwtService("test-secret-key-only-for-unit-tests-32-chars-min");
  private final BearerAuthFilter filter = new BearerAuthFilter(jwtService);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_authenticate_using_the_authorization_header() throws Exception {
    var userId = UUID.randomUUID();
    var token = jwtService.generateToken(userId, UserRole.ADMIN);

    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    var chain = mock(FilterChain.class);
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

    filter.doFilterInternal(request, response, chain);

    var principal =
        (Principal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    assertThat(principal.userId()).isEqualTo(userId);
    assertThat(principal.role()).isEqualTo(UserRole.ADMIN);
    verify(chain).doFilter(request, response);
  }

  @Test
  void should_authenticate_using_the_access_token_query_param_when_header_is_missing()
      throws Exception {
    var userId = UUID.randomUUID();
    var token = jwtService.generateToken(userId, UserRole.ADMIN);

    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    var chain = mock(FilterChain.class);
    when(request.getHeader("Authorization")).thenReturn(null);
    when(request.getParameter("access_token")).thenReturn(token);

    filter.doFilterInternal(request, response, chain);

    var principal =
        (Principal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    assertThat(principal.userId()).isEqualTo(userId);
    verify(chain).doFilter(request, response);
  }

  @Test
  void should_prefer_the_authorization_header_over_the_query_param() throws Exception {
    var headerUserId = UUID.randomUUID();
    var headerToken = jwtService.generateToken(headerUserId, UserRole.TEACHER);
    var queryToken = jwtService.generateToken(UUID.randomUUID(), UserRole.ADMIN);

    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    var chain = mock(FilterChain.class);
    when(request.getHeader("Authorization")).thenReturn("Bearer " + headerToken);
    when(request.getParameter("access_token")).thenReturn(queryToken);

    filter.doFilterInternal(request, response, chain);

    var principal =
        (Principal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    assertThat(principal.userId()).isEqualTo(headerUserId);
    assertThat(principal.role()).isEqualTo(UserRole.TEACHER);
  }

  @Test
  void should_leave_context_unauthenticated_when_no_token_is_provided() throws Exception {
    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    var chain = mock(FilterChain.class);
    when(request.getHeader("Authorization")).thenReturn(null);
    when(request.getParameter("access_token")).thenReturn(null);

    filter.doFilterInternal(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(request, response);
  }

  @Test
  void should_clear_context_for_an_invalid_query_param_token() throws Exception {
    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    var chain = mock(FilterChain.class);
    when(request.getHeader("Authorization")).thenReturn(null);
    when(request.getParameter("access_token")).thenReturn("not-a-real-token");

    filter.doFilterInternal(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(request, response);
  }
}
