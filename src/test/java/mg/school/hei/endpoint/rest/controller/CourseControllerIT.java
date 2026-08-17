package mg.school.hei.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.time.Instant;
import java.util.UUID;
import mg.school.hei.conf.FacadeIT;
import mg.school.hei.conf.RestTemplateTestConfig;
import mg.school.hei.endpoint.rest.controller.dto.CourseRequest;
import mg.school.hei.endpoint.rest.controller.dto.CourseResponse;
import mg.school.hei.model.UserRole;
import mg.school.hei.repository.AppUserRepository;
import mg.school.hei.repository.CourseRepository;
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
class CourseControllerIT extends FacadeIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private CourseRepository courseRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private JwtService jwtService;
    private String token;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
        var admin =
                appUserRepository.save(
                        JAppUser.builder()
                                .firstName("Admin")
                                .lastName("Course")
                                .email("admin-course-" + UUID.randomUUID() + "@example.com")
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

    private static String shortRef(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void creating_a_course_should_return_201() {
        var response =
                restTemplate.exchange(
                        "/courses",
                        HttpMethod.POST,
                        new HttpEntity<>(new CourseRequest(shortRef("PROG4"), "Qualite", 6), authHeaders()),
                        CourseResponse.class);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(6, response.getBody().credits());
    }

    @Test
    void creating_a_duplicate_ref_should_return_409() {
        String ref = shortRef("DUP");
        restTemplate.exchange(
                "/courses",
                HttpMethod.POST,
                new HttpEntity<>(new CourseRequest(ref, "Qualite", 6), authHeaders()),
                CourseResponse.class);
        var response =
                restTemplate.exchange(
                        "/courses",
                        HttpMethod.POST,
                        new HttpEntity<>(new CourseRequest(ref, "Qualite", 6), authHeaders()),
                        Object.class);

        assertEquals(409, response.getStatusCode().value());
    }

    @Test
    void getting_an_unknown_course_should_return_404() {
        var response = restTemplate.getForEntity("/courses/" + UUID.randomUUID(), Object.class);
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void deleting_a_course_without_assignments_should_return_204() {
        var created =
                restTemplate.exchange(
                        "/courses",
                        HttpMethod.POST,
                        new HttpEntity<>(new CourseRequest(shortRef("DEL"), "Qualite", 6), authHeaders()),
                        CourseResponse.class);

        var response =
                restTemplate.exchange(
                        "/courses/" + created.getBody().id(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, authHeaders()),
                        Void.class);

        assertEquals(204, response.getStatusCode().value());
    }

    @Test
    void updating_a_course_should_return_the_new_values() {
        var created =
                restTemplate.exchange(
                        "/courses",
                        HttpMethod.POST,
                        new HttpEntity<>(new CourseRequest(shortRef("UPD"), "Old", 3), authHeaders()),
                        CourseResponse.class);

        var response =
                restTemplate.exchange(
                        "/courses/" + created.getBody().id(),
                        HttpMethod.PATCH,
                        new HttpEntity<>(new CourseRequest(shortRef("UPD"), "New title", 5), authHeaders()),
                        CourseResponse.class);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("New title", response.getBody().title());
    }
}
