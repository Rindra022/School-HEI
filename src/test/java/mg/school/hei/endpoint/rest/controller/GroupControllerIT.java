package mg.school.hei.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.UUID;
import mg.school.hei.conf.FacadeIT;
import mg.school.hei.conf.RestTemplateTestConfig;
import mg.school.hei.endpoint.rest.controller.dto.*;
import mg.school.hei.model.Track;
import mg.school.hei.model.UserRole;
import mg.school.hei.repository.AppGroupRepository;
import mg.school.hei.repository.AppUserRepository;
import mg.school.hei.repository.PromotionRepository;
import mg.school.hei.repository.StudentRepository;
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
class GroupControllerIT extends FacadeIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private AppGroupRepository appGroupRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PromotionRepository promotionRepository;
    @Autowired private StudentRepository studentRepository;

    private HttpHeaders authHeaders;

    @BeforeEach
    void setUp() {
        var promotion = promotionRepository.save(JPromotion.builder().year(2024 + (int)(Math.random() * 1000)).build());
        String email = "group-auth-" + UUID.randomUUID() + "@example.com";
        restTemplate.postForEntity(
                "/register",
                new RegisterRequest("Grp", "Tester", null, email, "password123", null, promotion.getId()),
                Void.class);
        var login =
                restTemplate.postForEntity(
                        "/login", new LoginRequest(email, "password123"), AuthResponse.class);
        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(login.getBody().token());
    }

    @AfterEach
    void tearDown() {
        appGroupRepository.deleteAll();
        studentRepository.deleteAll();
        appUserRepository.deleteAll();
        promotionRepository.deleteAll();
    }

    @Test
    void listing_groups_without_auth_should_return_401() {
        var response = restTemplate.getForEntity("/groups", Object.class);
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void creating_a_group_should_return_201() {
        var response =
                restTemplate.exchange(
                        "/groups",
                        HttpMethod.POST,
                        new HttpEntity<>(new GroupRequest("K1", Track.EL), authHeaders),
                        GroupResponse.class);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(Track.EL, response.getBody().track());
    }

    @Test
    void deleting_an_unreferenced_group_should_return_204() {
        var created =
                restTemplate.exchange(
                        "/groups",
                        HttpMethod.POST,
                        new HttpEntity<>(new GroupRequest("K2", Track.TN), authHeaders),
                        GroupResponse.class);

        var response =
                restTemplate.exchange(
                        "/groups/" + created.getBody().id(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(authHeaders),
                        Void.class);

        assertEquals(204, response.getStatusCode().value());
    }

    @Test
    void getting_an_unknown_group_should_return_404() {
        var response =
                restTemplate.exchange(
                        "/groups/" + UUID.randomUUID(),
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders),
                        Object.class);
        assertEquals(404, response.getStatusCode().value());
    }
}