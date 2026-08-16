package mg.school.hei.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import mg.school.hei.conf.FacadeIT;
import mg.school.hei.conf.RestTemplateTestConfig;
import mg.school.hei.endpoint.rest.controller.dto.*;
import mg.school.hei.repository.PromotionRepository;
import mg.school.hei.repository.model.JPromotion;
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
class AuthControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private PromotionRepository promotionRepository;

  private java.util.UUID promotionId;

  @BeforeEach
  void setUp() {
    promotionId = promotionRepository.save(JPromotion.builder().year(2024).build()).getId();
  }

  @AfterEach
  void tearDown() {
    promotionRepository.deleteAll();
  }

  @Test
  void register_then_login_should_return_a_usable_token() {
    var registerRequest =
        new RegisterRequest(
            "Alice", "Doe", null, "alice@example.com", "password123", null, promotionId);
    var registerResponse = restTemplate.postForEntity("/register", registerRequest, Void.class);
    assertEquals(201, registerResponse.getStatusCode().value());

    var loginResponse =
        restTemplate.postForEntity(
            "/login", new LoginRequest("alice@example.com", "password123"), AuthResponse.class);
    assertEquals(200, loginResponse.getStatusCode().value());
    assertNotNull(loginResponse.getBody().token());
  }

  @Test
  void register_should_generate_a_std_prefixed_with_the_promotion_year() {
    var request =
        new RegisterRequest(
            "Bob", "K", null, "bob-std@example.com", "password123", null, promotionId);
    restTemplate.postForEntity("/register", request, Void.class);

    var login =
        restTemplate.postForEntity(
            "/login", new LoginRequest("bob-std@example.com", "password123"), AuthResponse.class);
    var headers = new HttpHeaders();
    headers.setBearerAuth(login.getBody().token());
    var me =
        restTemplate.exchange("/me", HttpMethod.GET, new HttpEntity<>(headers), MeResponse.class);

    assertEquals(200, me.getStatusCode().value());
    assertTrue(me.getBody().std().startsWith("STD24"));
  }

  @Test
  void login_with_wrong_password_should_return_401() {
    restTemplate.postForEntity(
        "/register",
        new RegisterRequest(
            "Bob", "Smith", null, "bob@example.com", "correct-pw", null, promotionId),
        Void.class);

    var response =
        restTemplate.postForEntity(
            "/login", new LoginRequest("bob@example.com", "wrong-pw"), Object.class);
    assertEquals(401, response.getStatusCode().value());
  }

  @Test
  void register_with_already_used_email_should_return_400() {
    var request =
        new RegisterRequest(
            "Alice", "Doe", null, "dup@example.com", "password123", null, promotionId);
    restTemplate.postForEntity("/register", request, Void.class);

    var response = restTemplate.postForEntity("/register", request, Object.class);
    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  void register_with_unknown_promotion_should_return_400() {
    var request =
        new RegisterRequest(
            "Nobody",
            "Nowhere",
            null,
            "nobody@example.com",
            "password123",
            null,
            java.util.UUID.randomUUID());
    var response = restTemplate.postForEntity("/register", request, Object.class);
    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  void me_without_token_should_return_401() {
    var response = restTemplate.getForEntity("/me", Object.class);
    assertEquals(401, response.getStatusCode().value());
  }
}
