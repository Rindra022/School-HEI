package mg.school.hei.service;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.AuthResponse;
import mg.school.hei.endpoint.rest.controller.dto.LoginRequest;
import mg.school.hei.endpoint.rest.controller.dto.MeResponse;
import mg.school.hei.endpoint.rest.controller.dto.RegisterRequest;
import mg.school.hei.exception.ResourceNotFoundException;
import mg.school.hei.model.UserRole;
import mg.school.hei.repository.AppUserRepository;
import mg.school.hei.repository.PromotionRepository;
import mg.school.hei.repository.StudentCounterRepository;
import mg.school.hei.repository.StudentRepository;
import mg.school.hei.repository.model.JAppUser;
import mg.school.hei.repository.model.JPromotion;
import mg.school.hei.repository.model.JStudent;
import mg.school.hei.security.jwt.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final AppUserRepository appUserRepository;
  private final StudentRepository studentRepository;
  private final PromotionRepository promotionRepository;
  private final StudentCounterRepository studentCounterRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  @Transactional
  public void register(RegisterRequest request) {
    if (appUserRepository.findByEmail(request.email()).isPresent()) {
      throw new IllegalArgumentException("Email already registered");
    }

    JPromotion promotion =
        promotionRepository
            .findById(request.promotionId())
            .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

    JAppUser user =
        appUserRepository.save(
            JAppUser.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .birthdate(request.birthdate())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .role(UserRole.STUDENT)
                .createdAt(Instant.now())
                .build());

    int year = promotion.getYear();
    int count = studentCounterRepository.incrementAndGet(year);
    String std = "STD" + String.format("%02d", year % 100) + String.format("%03d", count);
    studentRepository.save(
        JStudent.builder().id(user.getId()).std(std).promotion(promotion).build());
  }

  public AuthResponse login(LoginRequest request) {
    JAppUser user =
        appUserRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new BadCredentialsException("Invalid credentials");
    }

    return new AuthResponse(jwtService.generateToken(user.getId(), user.getRole()));
  }

  public MeResponse getCurrentUser(UUID userId) {
    JAppUser user =
        appUserRepository
            .findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found"));

    String std =
        user.getRole() == UserRole.STUDENT
            ? studentRepository.findById(userId).map(JStudent::getStd).orElse(null)
            : null;

    return new MeResponse(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        user.getRole(),
        std);
  }
}
