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
  private String token;

  @BeforeEach
  void setUp() {
    promotionRepository.deleteAll();
    var admin =
        appUserRepository.save(
            JAppUser.builder()
                .firstName("Admin")
                .lastName("Test")
                .email("admin-promo-" + UUID.randomUUID() + "@example.com")
                .password("hashed")
                .role(UserRole.ADMIN)
                .createdAt(Instant.now())
                .build());
    token = jwtService.generateToken(admin.getId(), admin.getRole());
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  @Test
  void creating_a_promotion_should_return_201() {
    var response =
        restTemplate.exchange(
            "/promotions",
            HttpMethod.POST,
            new HttpEntity<>(new PromotionRequest(2026), authHeaders()),
            PromotionResponse.class);

    assertEquals(201, response.getStatusCode().value());
    assertEquals(2026, response.getBody().year());
  }

  @Test
  void creating_a_duplicate_year_should_return_400() {
    restTemplate.exchange(
        "/promotions",
        HttpMethod.POST,
        new HttpEntity<>(new PromotionRequest(2027), authHeaders()),
        PromotionResponse.class);
    var response =
        restTemplate.exchange(
            "/promotions",
            HttpMethod.POST,
            new HttpEntity<>(new PromotionRequest(2027), authHeaders()),
            Object.class);

    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  void listing_promotions_should_return_200() {
    restTemplate.exchange(
        "/promotions",
        HttpMethod.POST,
        new HttpEntity<>(new PromotionRequest(2028), authHeaders()),
        PromotionResponse.class);

    var response = restTemplate.getForEntity("/promotions", PromotionResponse[].class);

    assertEquals(200, response.getStatusCode().value());
    assertTrue(response.getBody().length >= 1);
  }

  @Test
  void updating_an_unknown_promotion_should_return_404() {
    var response =
        restTemplate.exchange(
            "/promotions/" + UUID.randomUUID(),
            HttpMethod.PATCH,
            new HttpEntity<>(new PromotionRequest(2030), authHeaders()),
            Object.class);

    assertEquals(404, response.getStatusCode().value());
  }

  @Test
  void deleting_a_promotion_without_students_should_return_204() {
    var created =
        restTemplate.exchange(
            "/promotions",
            HttpMethod.POST,
            new HttpEntity<>(new PromotionRequest(2029), authHeaders()),
            PromotionResponse.class);

    var response =
        restTemplate.exchange(
            "/promotions/" + created.getBody().id(),
            HttpMethod.DELETE,
            new HttpEntity<>(null, authHeaders()),
            Void.class);

    assertEquals(204, response.getStatusCode().value());
  }
}
