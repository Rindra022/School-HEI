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
class CourseControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private CourseRepository courseRepository;
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
                .lastName("Course")
                .email("course-admin-" + UUID.randomUUID() + "@example.com")
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
                .lastName("Course")
                .email("course-student-" + UUID.randomUUID() + "@example.com")
                .password("hashed")
                .role(UserRole.STUDENT)
                .createdAt(Instant.now())
                .build());
    studentHeaders = new HttpHeaders();
    studentHeaders.setBearerAuth(jwtService.generateToken(student.getId(), student.getRole()));
  }

  @AfterEach
  void tearDown() {
    courseRepository.deleteAll();
    appUserRepository.deleteAll();
  }

  @Test
  void creating_a_course_as_admin_should_return_201() {
    var response =
        restTemplate.exchange(
            "/courses",
            HttpMethod.POST,
            new HttpEntity<>(
                new CourseRequest("PROG4-" + UUID.randomUUID(), "Qualite", 6), adminHeaders),
            CourseResponse.class);

    assertEquals(201, response.getStatusCode().value());
    assertEquals(6, response.getBody().credits());
  }

  @Test
  void creating_a_course_as_student_should_return_403() {
    var response =
        restTemplate.exchange(
            "/courses",
            HttpMethod.POST,
            new HttpEntity<>(
                new CourseRequest("PROG5-" + UUID.randomUUID(), "Qualite", 6), studentHeaders),
            Object.class);

    assertEquals(403, response.getStatusCode().value());
  }

  @Test
  void creating_a_duplicate_ref_should_return_409() {
    String ref = "DUP-" + UUID.randomUUID();
    restTemplate.exchange(
        "/courses",
        HttpMethod.POST,
        new HttpEntity<>(new CourseRequest(ref, "Qualite", 6), adminHeaders),
        CourseResponse.class);

    var response =
        restTemplate.exchange(
            "/courses",
            HttpMethod.POST,
            new HttpEntity<>(new CourseRequest(ref, "Qualite", 6), adminHeaders),
            Object.class);

    assertEquals(409, response.getStatusCode().value());
  }

  @Test
  void getting_an_unknown_course_without_auth_should_return_404() {
    var response = restTemplate.getForEntity("/courses/" + UUID.randomUUID(), Object.class);
    assertEquals(404, response.getStatusCode().value());
  }

  @Test
  void deleting_a_course_without_assignments_as_admin_should_return_204() {
    var created =
        restTemplate.exchange(
            "/courses",
            HttpMethod.POST,
            new HttpEntity<>(
                new CourseRequest("DEL-" + UUID.randomUUID(), "Qualite", 6), adminHeaders),
            CourseResponse.class);

    var response =
        restTemplate.exchange(
            "/courses/" + created.getBody().id(),
            HttpMethod.DELETE,
            new HttpEntity<>(adminHeaders),
            Void.class);

    assertEquals(204, response.getStatusCode().value());
  }

  @Test
  void deleting_a_course_as_student_should_return_403() {
    var created =
        restTemplate.exchange(
            "/courses",
            HttpMethod.POST,
            new HttpEntity<>(
                new CourseRequest("DEL2-" + UUID.randomUUID(), "Qualite", 6), adminHeaders),
            CourseResponse.class);

    var response =
        restTemplate.exchange(
            "/courses/" + created.getBody().id(),
            HttpMethod.DELETE,
            new HttpEntity<>(studentHeaders),
            Object.class);

    assertEquals(403, response.getStatusCode().value());
  }

  @Test
  void updating_a_course_as_admin_should_return_the_new_values() {
    var created =
        restTemplate.exchange(
            "/courses",
            HttpMethod.POST,
            new HttpEntity<>(new CourseRequest("UPD-" + UUID.randomUUID(), "Old", 3), adminHeaders),
            CourseResponse.class);

    var response =
        restTemplate.exchange(
            "/courses/" + created.getBody().id(),
            HttpMethod.PATCH,
            new HttpEntity<>(new CourseRequest("UPD-NEW", "New title", 5), adminHeaders),
            CourseResponse.class);

    assertEquals(200, response.getStatusCode().value());
    assertEquals("New title", response.getBody().title());
  }
}
