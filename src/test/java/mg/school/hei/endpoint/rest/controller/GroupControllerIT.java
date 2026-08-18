package mg.school.hei.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.time.Instant;
import java.util.UUID;
import mg.school.hei.conf.FacadeIT;
import mg.school.hei.conf.RestTemplateTestConfig;
import mg.school.hei.endpoint.rest.controller.dto.GroupRequest;
import mg.school.hei.endpoint.rest.controller.dto.GroupResponse;
import mg.school.hei.model.Track;
import mg.school.hei.model.UserRole;
import mg.school.hei.repository.AppGroupRepository;
import mg.school.hei.repository.AppUserRepository;
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
class GroupControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private AppGroupRepository appGroupRepository;
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
                .lastName("Group")
                .email("group-admin-" + UUID.randomUUID() + "@example.com")
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
                .lastName("Group")
                .email("group-student-" + UUID.randomUUID() + "@example.com")
                .password("hashed")
                .role(UserRole.STUDENT)
                .createdAt(Instant.now())
                .build());
    studentHeaders = new HttpHeaders();
    studentHeaders.setBearerAuth(jwtService.generateToken(student.getId(), student.getRole()));
  }

  @AfterEach
  void tearDown() {
    appGroupRepository.deleteAll();
    appUserRepository.deleteAll();
  }

  @Test
  void listing_groups_without_auth_should_return_401() {
    var response = restTemplate.getForEntity("/groups", Object.class);
    assertEquals(401, response.getStatusCode().value());
  }

  @Test
  void listing_groups_as_student_should_return_200() {
    var response =
        restTemplate.exchange(
            "/groups", HttpMethod.GET, new HttpEntity<>(studentHeaders), GroupResponse[].class);
    assertEquals(200, response.getStatusCode().value());
  }

  @Test
  void creating_a_group_as_admin_should_return_201() {
    var response =
        restTemplate.exchange(
            "/groups",
            HttpMethod.POST,
            new HttpEntity<>(new GroupRequest("K1", Track.EL), adminHeaders),
            GroupResponse.class);

    assertEquals(201, response.getStatusCode().value());
    assertEquals(Track.EL, response.getBody().track());
  }

  @Test
  void creating_a_group_as_student_should_return_403() {
    var response =
        restTemplate.exchange(
            "/groups",
            HttpMethod.POST,
            new HttpEntity<>(new GroupRequest("K9", Track.TN), studentHeaders),
            Object.class);

    assertEquals(403, response.getStatusCode().value());
  }

  @Test
  void deleting_an_unreferenced_group_as_admin_should_return_204() {
    var created =
        restTemplate.exchange(
            "/groups",
            HttpMethod.POST,
            new HttpEntity<>(new GroupRequest("K2", Track.TN), adminHeaders),
            GroupResponse.class);

    var response =
        restTemplate.exchange(
            "/groups/" + created.getBody().id(),
            HttpMethod.DELETE,
            new HttpEntity<>(adminHeaders),
            Void.class);

    assertEquals(204, response.getStatusCode().value());
  }

  @Test
  void deleting_a_group_as_student_should_return_403() {
    var created =
        restTemplate.exchange(
            "/groups",
            HttpMethod.POST,
            new HttpEntity<>(new GroupRequest("K3", Track.TN), adminHeaders),
            GroupResponse.class);

    var response =
        restTemplate.exchange(
            "/groups/" + created.getBody().id(),
            HttpMethod.DELETE,
            new HttpEntity<>(studentHeaders),
            Object.class);

    assertEquals(403, response.getStatusCode().value());
  }

  @Test
  void getting_an_unknown_group_as_admin_should_return_404() {
    var response =
        restTemplate.exchange(
            "/groups/" + UUID.randomUUID(),
            HttpMethod.GET,
            new HttpEntity<>(adminHeaders),
            Object.class);
    assertEquals(404, response.getStatusCode().value());
  }
}
