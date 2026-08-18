package mg.school.hei.endpoint.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import mg.school.hei.conf.FacadeIT;
import mg.school.hei.conf.RestTemplateTestConfig;
import mg.school.hei.repository.PromotionRepository;
import mg.school.hei.repository.model.JPromotion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(RestTemplateTestConfig.class)
class PromotionViewControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private PromotionRepository promotionRepository;

  @BeforeEach
  void setUp() {
    promotionRepository.save(JPromotion.builder().year(2025).build());
  }

  @AfterEach
  void tearDown() {
    promotionRepository.deleteAll();
  }

  @Test
  void promotions_view_should_return_200_without_authentication() {
    var response = restTemplate.getForEntity("/promotions-view", String.class);

    assertEquals(200, response.getStatusCode().value());
  }

  @Test
  void promotions_view_should_list_the_promotion_year() {
    var response = restTemplate.getForEntity("/promotions-view", String.class);

    assertTrue(response.getBody().contains("2025"));
  }

  @Test
  void promotions_view_should_include_the_download_button() {
    var response = restTemplate.getForEntity("/promotions-view", String.class);

    assertTrue(response.getBody().contains("Télécharger la liste des diplômés"));
    assertTrue(response.getBody().contains("downloadGraduates"));
  }

  @Test
  void promotions_view_should_include_the_admin_login_form() {
    var response = restTemplate.getForEntity("/promotions-view", String.class);

    assertTrue(response.getBody().contains("Connexion ADMIN"));
  }
}
