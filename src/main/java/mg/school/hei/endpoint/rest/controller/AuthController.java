package mg.school.hei.endpoint.rest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.AuthResponse;
import mg.school.hei.endpoint.rest.controller.dto.LoginRequest;
import mg.school.hei.endpoint.rest.controller.dto.RegisterRequest;
import mg.school.hei.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
    authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }
}
