package mg.school.hei.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.math.BigDecimal;
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
class GraduateControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private AppUserRepository appUserRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private AppGroupRepository appGroupRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private CourseAssignmentRepository courseAssignmentRepository;
  @Autowired private GroupMembershipRepository groupMembershipRepository;
  @Autowired private GradeRepository gradeRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private JwtService jwtService;

  private String token;
  private UUID promotionId;
  private UUID graduatingStudentId;

  @BeforeEach
  void setUp() {
    var promotion = promotionRepository.save(JPromotion.builder().year(2024).build());
    promotionId = promotion.getId();

    var admin =
        appUserRepository.save(
            JAppUser.builder()
                .firstName("Admin")
                .lastName("Test")
                .email("admin-" + UUID.randomUUID() + "@example.com")
                .password("hashed")
                .role(UserRole.ADMIN)
                .createdAt(Instant.now())
                .build());
    token = jwtService.generateToken(admin.getId(), admin.getRole());

    var studentUser =
        appUserRepository.save(
            JAppUser.builder()
                .firstName("Grad")
                .lastName("Uate")
                .email("grad-" + UUID.randomUUID() + "@example.com")
                .password("hashed")
                .role(UserRole.STUDENT)
                .createdAt(Instant.now())
                .build());
    graduatingStudentId = studentUser.getId();
    studentRepository.save(
        JStudent.builder().id(studentUser.getId()).std("STD24070").promotion(promotion).build());

    var teacher =
        appUserRepository.save(
            JAppUser.builder()
                .firstName("Teacher")
                .lastName("Graduate")
                .email("grad-teacher-" + UUID.randomUUID() + "@example.com")
                .password("hashed")
                .role(UserRole.TEACHER)
                .createdAt(Instant.now())
                .build());

    var group =
        appGroupRepository.save(
            JAppGroup.builder()
                .ref("K1-" + UUID.randomUUID().toString().substring(0, 8))
                .track(Track.EL)
                .build());
    var course =
        courseRepository.save(
            JCourse.builder()
                .ref("PROG4-" + UUID.randomUUID().toString().substring(0, 8))
                .title("Qualite")
                .credits(6)
                .build());
    var assignment =
        courseAssignmentRepository.save(
            JCourseAssignment.builder()
                .course(course)
                .teacher(teacher)
                .group(group)
                .academicYear(2024)
                .build());

    var groupMembershipRequest =
        new GroupMembershipRequest(graduatingStudentId, group.getId(), LocalDate.of(2024, 9, 1));
    restTemplate.exchange(
        "/group-memberships",
        HttpMethod.POST,
        new HttpEntity<>(groupMembershipRequest, authHeaders()),
        GroupMembershipResponse.class);

    var examResponse =
        restTemplate.exchange(
            "/exams",
            HttpMethod.POST,
            new HttpEntity<>(
                new ExamRequest(assignment.getId(), Instant.now(), BigDecimal.ONE), authHeaders()),
            ExamResponse.class);

    restTemplate.exchange(
        "/grades",
        HttpMethod.POST,
        new HttpEntity<>(
            new GradeRequest(
                graduatingStudentId, examResponse.getBody().id(), new BigDecimal("16.00"), null),
            authHeaders()),
        GradeResponse.class);
  }

  @AfterEach
  void tearDown() {
    gradeRepository.deleteAll();
    groupMembershipRepository.deleteAll();
    examRepository.deleteAll();
    courseAssignmentRepository.deleteAll();
    courseRepository.deleteAll();
    appGroupRepository.deleteAll();
    studentRepository.deleteAll();
    promotionRepository.deleteAll();
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  @Test
  void listGraduates_should_return_the_student_with_a_full_validated_transcript() {
    var response =
        restTemplate.exchange(
            "/promotions/" + promotionId + "/graduates",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            GraduateResponse[].class);

    assertEquals(200, response.getStatusCode().value());
    assertEquals(1, response.getBody().length);
    assertEquals("STD24070", response.getBody()[0].std());
    assertEquals(1, response.getBody()[0].rank());
    assertEquals(Track.EL, response.getBody()[0].track());
  }

  @Test
  void listGraduates_with_track_filter_should_exclude_other_tracks() {
    var response =
        restTemplate.exchange(
            "/promotions/" + promotionId + "/graduates?track=TN",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            GraduateResponse[].class);

    assertEquals(200, response.getStatusCode().value());
    assertEquals(0, response.getBody().length);
  }
}
