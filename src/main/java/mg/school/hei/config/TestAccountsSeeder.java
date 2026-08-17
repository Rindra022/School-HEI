package mg.school.hei.config;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.RegisterRequest;
import mg.school.hei.model.UserRole;
import mg.school.hei.repository.AppUserRepository;
import mg.school.hei.repository.PromotionRepository;
import mg.school.hei.repository.model.JAppUser;
import mg.school.hei.repository.model.JPromotion;
import mg.school.hei.service.AuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seed-test-accounts", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class TestAccountsSeeder implements CommandLineRunner {
  private static final String TEST_PASSWORD = "password123";

  private final AppUserRepository appUserRepository;
  private final PromotionRepository promotionRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthService authService;

  @Override
  public void run(String... args) {
    seedStaff("admin@hei.school", "Admin", UserRole.ADMIN);
    seedStaff("teacher@hei.school", "Teacher", UserRole.TEACHER);
    seedStudent();
  }

  private void seedStaff(String email, String firstName, UserRole role) {
    if (appUserRepository.findByEmail(email).isPresent()) return;
    appUserRepository.save(
        JAppUser.builder()
            .firstName(firstName)
            .lastName("Test")
            .email(email)
            .password(passwordEncoder.encode(TEST_PASSWORD))
            .role(role)
            .createdAt(Instant.now())
            .build());
  }

  private void seedStudent() {
    if (appUserRepository.findByEmail("student@hei.school").isPresent()) return;

    JPromotion promotion =
        promotionRepository.findByYear(2024).orElseGet(this::createTestPromotion);

    authService.register(
        new RegisterRequest(
            "Student", "Test", null, "student@hei.school", TEST_PASSWORD, null, promotion.getId()));
  }

  private JPromotion createTestPromotion() {
    return promotionRepository.save(JPromotion.builder().year(2024).build());
  }
}
