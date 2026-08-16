package mg.school.hei.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import mg.school.hei.endpoint.rest.controller.dto.LoginRequest;
import mg.school.hei.endpoint.rest.controller.dto.RegisterRequest;
import mg.school.hei.model.UserRole;
import mg.school.hei.repository.*;
import mg.school.hei.repository.model.JAppUser;
import mg.school.hei.repository.model.JPromotion;
import mg.school.hei.repository.model.JStudent;
import mg.school.hei.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

  private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
  private final StudentRepository studentRepository = mock(StudentRepository.class);
  private final PromotionRepository promotionRepository = mock(PromotionRepository.class);
  private final StudentCounterRepository studentCounterRepository =
      mock(StudentCounterRepository.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final JwtService jwtService = mock(JwtService.class);

  private final AuthService service =
      new AuthService(
          appUserRepository,
          studentRepository,
          promotionRepository,
          studentCounterRepository,
          passwordEncoder,
          jwtService);

  @Test
  void register_should_reject_an_already_used_email() {
    var promotionId = UUID.randomUUID();
    var request =
        new RegisterRequest(
            "Alice", "Doe", null, "alice@example.com", "pw12345678", null, promotionId);
    when(appUserRepository.findByEmail("alice@example.com"))
        .thenReturn(Optional.of(new JAppUser()));

    assertThatThrownBy(() -> service.register(request))
        .isInstanceOf(IllegalArgumentException.class);
    verify(appUserRepository, never()).save(any());
    verify(promotionRepository, never()).findById(any());
  }

  @Test
  void register_should_reject_an_unknown_promotion() {
    var promotionId = UUID.randomUUID();
    var request =
        new RegisterRequest("Bob", "K", null, "bob@example.com", "pw12345678", null, promotionId);

    when(appUserRepository.findByEmail("bob@example.com")).thenReturn(Optional.empty());
    when(promotionRepository.findById(promotionId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.register(request))
        .isInstanceOf(mg.school.hei.exception.ResourceNotFoundException.class);
  }

  @Test
  void register_should_generate_std_using_year_prefix_and_incremented_counter() {
    var promotionId = UUID.randomUUID();
    var promotion = JPromotion.builder().id(promotionId).year(2024).build();
    var request =
        new RegisterRequest(
            "Carol", "Ray", null, "carol@example.com", "pw12345678", "0341234567", promotionId);

    when(appUserRepository.findByEmail("carol@example.com")).thenReturn(Optional.empty());
    when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(promotion));
    when(passwordEncoder.encode("pw12345678")).thenReturn("hashed");
    when(appUserRepository.save(any()))
        .thenAnswer(
            inv -> inv.getArgument(0, JAppUser.class).toBuilder().id(UUID.randomUUID()).build());
    when(studentCounterRepository.incrementAndGet(2024)).thenReturn(7);

    service.register(request);

    var studentCaptor = ArgumentCaptor.forClass(JStudent.class);
    verify(studentRepository).save(studentCaptor.capture());
    assertThat(studentCaptor.getValue().getStd()).isEqualTo("STD24007");
  }

  @Test
  void login_should_reject_an_unknown_email() {
    var request = new LoginRequest("ghost@example.com", "whatever");
    when(appUserRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.login(request)).isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void login_should_reject_wrong_password() {
    var request = new LoginRequest("alice@example.com", "wrong-pw");
    var user = JAppUser.builder().email("alice@example.com").password("hashed").build();

    when(appUserRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong-pw", "hashed")).thenReturn(false);

    assertThatThrownBy(() -> service.login(request)).isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void login_should_return_a_token_on_valid_credentials() {
    var userId = UUID.randomUUID();
    var request = new LoginRequest("alice@example.com", "correct-pw");
    var user =
        JAppUser.builder()
            .id(userId)
            .email("alice@example.com")
            .password("hashed")
            .role(UserRole.STUDENT)
            .build();

    when(appUserRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("correct-pw", "hashed")).thenReturn(true);
    when(jwtService.generateToken(userId, UserRole.STUDENT)).thenReturn("fake-token");

    var response = service.login(request);

    assertThat(response.token()).isEqualTo("fake-token");
  }

  @Test
  void getCurrentUser_should_return_null_std_for_non_students() {
    var userId = UUID.randomUUID();
    var teacher =
        JAppUser.builder()
            .id(userId)
            .firstName("T")
            .lastName("Cher")
            .email("t@example.com")
            .role(UserRole.TEACHER)
            .build();
    when(appUserRepository.findById(userId)).thenReturn(Optional.of(teacher));

    var response = service.getCurrentUser(userId);

    assertThat(response.std()).isNull();
    verify(studentRepository, never()).findById(any());
  }
}
