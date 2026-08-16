package mg.school.hei.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import mg.school.hei.conf.FacadeIT;
import mg.school.hei.conf.RestTemplateTestConfig;
import mg.school.hei.endpoint.rest.controller.dto.*;
import mg.school.hei.model.Track;
import mg.school.hei.model.UserRole;
import mg.school.hei.repository.*;
import mg.school.hei.repository.model.*;
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
class GroupMembershipControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private AppUserRepository appUserRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private AppGroupRepository appGroupRepository;
  @Autowired private GroupMembershipRepository groupMembershipRepository;

  private HttpHeaders authHeaders;
  private UUID studentId;
  private UUID groupAId;
  private UUID groupBId;

  @BeforeEach
  void setUp() {
    var promotion = promotionRepository.save(JPromotion.builder().year(2024).build());
    var user =
        appUserRepository.save(
            JAppUser.builder()
                .firstName("Rindra")
                .lastName("Student")
                .email("gm-" + UUID.randomUUID() + "@example.com")
                .password("hashed")
                .role(UserRole.STUDENT)
                .createdAt(Instant.now())
                .build());
    studentRepository.save(
        JStudent.builder().id(user.getId()).std("STD24099").promotion(promotion).build());
    studentId = user.getId();

    groupAId =
        appGroupRepository
            .save(
                JAppGroup.builder()
                    .ref("K1-" + UUID.randomUUID().toString().substring(0, 8))
                    .track(Track.EL)
                    .build())
            .getId();
    groupBId =
        appGroupRepository
            .save(
                JAppGroup.builder()
                    .ref("K3-" + UUID.randomUUID().toString().substring(0, 8))
                    .track(Track.EL)
                    .build())
            .getId();

    String email = "gm-auth-" + UUID.randomUUID() + "@example.com";
    restTemplate.postForEntity(
        "/register",
        new RegisterRequest("GM", "Tester", null, email, "password123", null, promotion.getId()),
        Void.class);
    var login =
        restTemplate.postForEntity(
            "/login", new LoginRequest(email, "password123"), AuthResponse.class);
    authHeaders = new HttpHeaders();
    authHeaders.setBearerAuth(login.getBody().token());
  }

  @AfterEach
  void tearDown() {
    groupMembershipRepository.deleteAll();
    appGroupRepository.deleteAll();
    studentRepository.deleteAll();
    appUserRepository.deleteAll();
    promotionRepository.deleteAll();
  }

  @Test
  void moving_a_student_to_a_new_group_should_close_the_previous_membership() {
    post(new GroupMembershipRequest(studentId, groupAId, LocalDate.of(2024, 9, 1)));
    post(new GroupMembershipRequest(studentId, groupBId, LocalDate.of(2024, 11, 15)));

    var history =
        restTemplate.exchange(
            "/group-memberships?studentId=" + studentId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders),
            GroupMembershipResponse[].class);

    assertEquals(2, history.getBody().length);
    assertEquals(LocalDate.of(2024, 11, 14), history.getBody()[0].endDate());
    assertNull(history.getBody()[1].endDate());
  }

  @Test
  void backdating_before_the_active_membership_should_return_400() {
    post(new GroupMembershipRequest(studentId, groupAId, LocalDate.of(2024, 11, 1)));

    var response =
        restTemplate.exchange(
            "/group-memberships",
            HttpMethod.POST,
            new HttpEntity<>(
                new GroupMembershipRequest(studentId, groupBId, LocalDate.of(2024, 9, 1)),
                authHeaders),
            Object.class);

    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  void create_with_unknown_group_should_return_404() {
    var response =
        restTemplate.exchange(
            "/group-memberships",
            HttpMethod.POST,
            new HttpEntity<>(
                new GroupMembershipRequest(studentId, UUID.randomUUID(), LocalDate.now()),
                authHeaders),
            Object.class);
    assertEquals(404, response.getStatusCode().value());
  }

  private void post(GroupMembershipRequest request) {
    var response =
        restTemplate.exchange(
            "/group-memberships",
            HttpMethod.POST,
            new HttpEntity<>(request, authHeaders),
            GroupMembershipResponse.class);
    assertEquals(201, response.getStatusCode().value());
  }
}
