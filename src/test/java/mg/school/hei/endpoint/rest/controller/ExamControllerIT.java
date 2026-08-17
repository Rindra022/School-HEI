package mg.school.hei.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import mg.school.hei.conf.FacadeIT;
import mg.school.hei.conf.RestTemplateTestConfig;
import mg.school.hei.endpoint.rest.controller.dto.*;
import mg.school.hei.model.Track;
import mg.school.hei.model.UserRole;
import mg.school.hei.repository.*;
import mg.school.hei.repository.PromotionRepository;
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

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(RestTemplateTestConfig.class)
class ExamControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private AppUserRepository appUserRepository;
  @Autowired private AppGroupRepository appGroupRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private CourseAssignmentRepository courseAssignmentRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private mg.school.hei.security.jwt.JwtService jwtService;

  private HttpHeaders authHeaders;
  private UUID assignmentId;

  @BeforeEach
  void setUp() {
    var teacher =
        appUserRepository.save(
            JAppUser.builder()
                .firstName("Teacher")
                .lastName("One")
                .email("exam-teacher-" + UUID.randomUUID() + "@example.com")
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
                .ref("PROG4-" + UUID.randomUUID().toString().substring(0, 6))
                .title("Qualite")
                .credits(6)
                .build());

    assignmentId =
        courseAssignmentRepository
            .save(
                JCourseAssignment.builder()
                    .course(course)
                    .teacher(teacher)
                    .group(group)
                    .academicYear(2024)
                    .build())
            .getId();

    authHeaders = new HttpHeaders();
    authHeaders.setBearerAuth(jwtService.generateToken(teacher.getId(), UserRole.TEACHER));
  }

  @AfterEach
  void tearDown() {
    examRepository.deleteAll();
    courseAssignmentRepository.deleteAll();
    courseRepository.deleteAll();
    appGroupRepository.deleteAll();
    studentRepository.deleteAll();
    appUserRepository.deleteAll();
    promotionRepository.deleteAll();
  }

  @Test
  void creating_an_exam_should_return_201() {
    var response = createExamRaw(new BigDecimal("0.50"));
    assertEquals(201, response.getStatusCode().value());
  }

  @Test
  void coefficients_exceeding_one_should_be_rejected() {
    createExam(new BigDecimal("0.60"));
    var response = createExamRaw(new BigDecimal("0.50"));
    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  void coefficients_summing_to_exactly_one_should_be_accepted() {
    createExam(new BigDecimal("0.40"));
    var response = createExamRaw(new BigDecimal("0.60"));
    assertEquals(201, response.getStatusCode().value());
  }

  @Test
  void create_with_unknown_assignment_should_return_404() {
    var request = new ExamRequest(UUID.randomUUID(), Instant.now(), new BigDecimal("0.50"));
    var response =
        restTemplate.exchange(
            "/exams",
            org.springframework.http.HttpMethod.POST,
            new HttpEntity<>(request, authHeaders),
            Object.class);
    assertEquals(404, response.getStatusCode().value());
  }

  @Test
  void listing_exams_should_return_only_the_assignment_ones() {
    createExam(new BigDecimal("0.30"));
    createExam(new BigDecimal("0.30"));

    var response =
        restTemplate.exchange(
            "/exams?assignmentId=" + assignmentId,
            org.springframework.http.HttpMethod.GET,
            new HttpEntity<>(authHeaders),
            ExamResponse[].class);

    assertEquals(200, response.getStatusCode().value());
    assertEquals(2, response.getBody().length);
  }

  @Test
  void deleting_an_exam_with_no_grades_should_return_204() {
    var examId = createExam(new BigDecimal("1.00"));

    var response =
        restTemplate.exchange(
            "/exams/" + examId,
            org.springframework.http.HttpMethod.DELETE,
            new HttpEntity<>(authHeaders),
            Void.class);

    assertEquals(204, response.getStatusCode().value());
  }

  @Test
  void deleting_an_unknown_exam_should_return_404() {
    var response =
        restTemplate.exchange(
            "/exams/" + UUID.randomUUID(),
            org.springframework.http.HttpMethod.DELETE,
            new HttpEntity<>(authHeaders),
            Object.class);
    assertEquals(404, response.getStatusCode().value());
  }

  private UUID createExam(BigDecimal coefficient) {
    var request = new ExamRequest(assignmentId, Instant.now(), coefficient);
    var response =
        restTemplate.exchange(
            "/exams",
            org.springframework.http.HttpMethod.POST,
            new HttpEntity<>(request, authHeaders),
            ExamResponse.class);
    assertEquals(201, response.getStatusCode().value());
    return response.getBody().id();
  }

  private org.springframework.http.ResponseEntity<Object> createExamRaw(BigDecimal coefficient) {
    var request = new ExamRequest(assignmentId, Instant.now(), coefficient);
    return restTemplate.exchange(
        "/exams",
        org.springframework.http.HttpMethod.POST,
        new HttpEntity<>(request, authHeaders),
        Object.class);
  }
}
