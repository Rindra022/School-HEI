package mg.school.hei.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.time.Instant;
import java.util.UUID;
import mg.school.hei.conf.FacadeIT;
import mg.school.hei.conf.RestTemplateTestConfig;
import mg.school.hei.endpoint.rest.controller.dto.PromotionRequest;
import mg.school.hei.endpoint.rest.controller.dto.PromotionResponse;
import mg.school.hei.model.UserRole;
import mg.school.hei.repository.AppUserRepository;
import mg.school.hei.repository.PromotionRepository;
import mg.school.hei.repository.model.JAppUser;
import mg.school.hei.security.jwt.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(RestTemplateTestConfig.class)
class PromotionControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private AppUserRepository appUserRepository;
  @Autowired private JwtService jwtService;

  private HttpHeaders adminHeaders;
  private HttpHeaders studentHeaders;

  @BeforeEach
  void setUp() {
    var admin =
        appUserRepository.save(
            JAppUser.builder()
                .firstName("Admin")
                .lastName("Promo")
                .email("promo-admin-" + UUID.randomUUID() + "@example.com")
                .password("hashed")
                .role(UserRole.ADMIN)
                .createdAt(Instant.now())
                .build());
    adminHeaders = new HttpHeaders();
    adminHeaders.setBearerAuth(jwtService.generateToken(admin.getId(), admin.getRole()));

    var student =
        appUserRepository.save(
            JAppUser.builder()
                .firstName("Student")
                .lastName("Promo")
                .email("promo-student-" + UUID.randomUUID() + "@example.com")
                .password("hashed")
                .role(UserRole.STUDENT)
                .createdAt(Instant.now())
                .build());
    studentHeaders = new HttpHeaders();
    studentHeaders.setBearerAuth(jwtService.generateToken(student.getId(), student.getRole()));
  }

  @AfterEach
  void tearDown() {
    promotionRepository.deleteAll();
    appUserRepository.deleteAll();
  }

  @Test
  void creating_a_promotion_as_admin_should_return_201() {
    var response =
        restTemplate.exchange(
            "/promotions",
            HttpMethod.POST,
            new HttpEntity<>(new PromotionRequest(2026), adminHeaders),
            PromotionResponse.class);

    assertEquals(201, response.getStatusCode().value());
    assertEquals(2026, response.getBody().year());
  }

  @Test
  void creating_a_promotion_as_student_should_return_403() {
    var response =
        restTemplate.exchange(
            "/promotions",
            HttpMethod.POST,
            new HttpEntity<>(new PromotionRequest(2031), studentHeaders),
            Object.class);

    assertEquals(403, response.getStatusCode().value());
  }

  @Test
  void creating_a_duplicate_year_should_return_400() {
    restTemplate.exchange(
        "/promotions",
        HttpMethod.POST,
        new HttpEntity<>(new PromotionRequest(2027), adminHeaders),
        PromotionResponse.class);

    var response =
        restTemplate.exchange(
            "/promotions",
            HttpMethod.POST,
            new HttpEntity<>(new PromotionRequest(2027), adminHeaders),
            Object.class);

    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  void listing_promotions_without_auth_should_return_200() {
    var response = restTemplate.getForEntity("/promotions", PromotionResponse[].class);
    assertEquals(200, response.getStatusCode().value());
  }

  @Test
  void updating_an_unknown_promotion_as_admin_should_return_404() {
    var response =
        restTemplate.exchange(
            "/promotions/" + UUID.randomUUID(),
            HttpMethod.PATCH,
            new HttpEntity<>(new PromotionRequest(2030), adminHeaders),
            Object.class);

    assertEquals(404, response.getStatusCode().value());
  }

  @Test
  void updating_a_promotion_as_student_should_return_403() {
    var created =
        restTemplate.exchange(
            "/promotions",
            HttpMethod.POST,
            new HttpEntity<>(new PromotionRequest(2032), adminHeaders),
            PromotionResponse.class);

    var response =
        restTemplate.exchange(
            "/promotions/" + created.getBody().id(),
            HttpMethod.PATCH,
            new HttpEntity<>(new PromotionRequest(2033), studentHeaders),
            Object.class);

    assertEquals(403, response.getStatusCode().value());
  }

  @Test
  void deleting_a_promotion_without_students_as_admin_should_return_204() {
    var created =
        restTemplate.exchange(
            "/promotions",
            HttpMethod.POST,
            new HttpEntity<>(new PromotionRequest(2029), adminHeaders),
            PromotionResponse.class);

    var response =
        restTemplate.exchange(
            "/promotions/" + created.getBody().id(),
            HttpMethod.DELETE,
            new HttpEntity<>(adminHeaders),
            Void.class);

    assertEquals(204, response.getStatusCode().value());
  }

  @Test
  void deleting_a_promotion_as_student_should_return_403() {
    var created =
        restTemplate.exchange(
            "/promotions",
            HttpMethod.POST,
            new HttpEntity<>(new PromotionRequest(2034), adminHeaders),
            PromotionResponse.class);

    var response =
        restTemplate.exchange(
            "/promotions/" + created.getBody().id(),
            HttpMethod.DELETE,
            new HttpEntity<>(studentHeaders),
            Object.class);

    assertEquals(403, response.getStatusCode().value());
  }
}
