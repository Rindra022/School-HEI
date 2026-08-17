package mg.school.hei.config;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import mg.school.hei.endpoint.rest.controller.dto.RegisterRequest;
import mg.school.hei.repository.AppUserRepository;
import mg.school.hei.repository.PromotionRepository;
import mg.school.hei.repository.model.JAppUser;
import mg.school.hei.repository.model.JPromotion;
import mg.school.hei.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class TestAccountsSeederTest {

  private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
  private final PromotionRepository promotionRepository = mock(PromotionRepository.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final AuthService authService = mock(AuthService.class);

  private final TestAccountsSeeder seeder =
      new TestAccountsSeeder(appUserRepository, promotionRepository, passwordEncoder, authService);

  @Test
  void run_creates_all_test_accounts_when_none_exist() {
    when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    when(promotionRepository.findByYear(2024)).thenReturn(Optional.empty());
    when(promotionRepository.save(any(JPromotion.class)))
        .thenReturn(JPromotion.builder().id(UUID.randomUUID()).year(2024).build());

    seeder.run();

    verify(appUserRepository, times(2)).save(any(JAppUser.class));
    verify(authService, times(1)).register(any(RegisterRequest.class));
  }

  @Test
  void run_skips_accounts_that_already_exist() {
    when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(mock(JAppUser.class)));

    seeder.run();

    verify(appUserRepository, never()).save(any(JAppUser.class));
    verify(authService, never()).register(any(RegisterRequest.class));
  }

  @Test
  void run_reuses_existing_2024_promotion_for_student_seed() {
    when(appUserRepository.findByEmail("admin@hei.school")).thenReturn(Optional.empty());
    when(appUserRepository.findByEmail("teacher@hei.school")).thenReturn(Optional.empty());
    when(appUserRepository.findByEmail("student@hei.school")).thenReturn(Optional.empty());
    when(passwordEncoder.encode(anyString())).thenReturn("encoded");

    JPromotion existing = JPromotion.builder().id(UUID.randomUUID()).year(2024).build();
    when(promotionRepository.findByYear(2024)).thenReturn(Optional.of(existing));

    seeder.run();

    verify(promotionRepository, never()).save(any(JPromotion.class));
    verify(authService).register(argThat(req -> req.promotionId().equals(existing.getId())));
  }
}
