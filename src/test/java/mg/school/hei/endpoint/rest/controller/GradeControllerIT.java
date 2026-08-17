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
class GradeControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private AppUserRepository appUserRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private AppGroupRepository appGroupRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private CourseAssignmentRepository courseAssignmentRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private GradeRepository gradeRepository;
  @Autowired private JwtService jwtService;

  private HttpHeaders authHeaders;
  private UUID studentId;
  private UUID examId;
  private HttpHeaders teacherHeaders;

  @BeforeEach
  void setUp() {
    var promotion = promotionRepository.save(JPromotion.builder().year(2024).build());

    var studentUser =
        appUserRepository.save(
            JAppUser.builder()
                .firstName("Grade")
                .lastName("Student")
                .email("grade-" + UUID.randomUUID() + "@example.com")
                .password("hashed")
                .role(UserRole.STUDENT)
                .createdAt(Instant.now())
                .build());
    studentRepository.save(
        JStudent.builder().id(studentUser.getId()).std("STD24050").promotion(promotion).build());
    studentId = studentUser.getId();

    var teacher =
        appUserRepository.save(
            JAppUser.builder()
                .firstName("Teacher")
                .lastName("Two")
                .email("grade-teacher-" + UUID.randomUUID() + "@example.com")
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
    var assignment =
        courseAssignmentRepository.save(
            JCourseAssignment.builder()
                .course(course)
                .teacher(teacher)
                .group(group)
                .academicYear(2024)
                .build());

    authHeaders = new HttpHeaders();
    authHeaders.setBearerAuth(jwtService.generateToken(studentUser.getId(), UserRole.STUDENT));

    teacherHeaders = new HttpHeaders();
    teacherHeaders.setBearerAuth(jwtService.generateToken(teacher.getId(), UserRole.TEACHER));
    var examResponse =
        restTemplate.exchange(
            "/exams",
            HttpMethod.POST,
            new HttpEntity<>(
                new ExamRequest(assignment.getId(), Instant.now(), BigDecimal.ONE), teacherHeaders),
            ExamResponse.class);
    examId = examResponse.getBody().id();
  }

  @AfterEach
  void tearDown() {
    gradeRepository.deleteAll();
    examRepository.deleteAll();
    courseAssignmentRepository.deleteAll();
    courseRepository.deleteAll();
    appGroupRepository.deleteAll();
    studentRepository.deleteAll();
    appUserRepository.deleteAll();
    promotionRepository.deleteAll();
  }

  @Test
  void recording_a_first_grade_should_return_201_without_reason() {
    var response = recordGradeRaw(new BigDecimal("12.00"), null);
    assertEquals(201, response.getStatusCode().value());
  }

  @Test
  void grading_twice_without_reason_should_return_400() {
    recordGrade(new BigDecimal("12.00"), null);
    var response = recordGradeRaw(new BigDecimal("15.00"), null);
    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  void amending_a_grade_with_a_reason_should_return_201_and_keep_full_history() {
    var firstId = recordGrade(new BigDecimal("8.00"), null);
    recordGrade(new BigDecimal("14.00"), "Erreur de transcription");

    var history =
        restTemplate.exchange(
            "/grades/" + firstId + "/history",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders),
            GradeResponse[].class);

    assertEquals(200, history.getStatusCode().value());
    assertEquals(2, history.getBody().length);
    assertEquals(new BigDecimal("8.00"), history.getBody()[0].value());
    assertEquals(new BigDecimal("14.00"), history.getBody()[1].value());
    assertFalse(history.getBody()[0].current());
    assertTrue(history.getBody()[1].current());
  }

  @Test
  void listing_grades_by_student_should_only_return_the_current_one() {
    recordGrade(new BigDecimal("9.00"), null);
    recordGrade(new BigDecimal("13.00"), "Reclamation");

    var response =
        restTemplate.exchange(
            "/grades?studentId=" + studentId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders),
            GradeResponse[].class);

    assertEquals(1, response.getBody().length);
    assertEquals(new BigDecimal("13.00"), response.getBody()[0].value());
  }

  @Test
  void listing_grades_by_exam_only_should_return_the_current_grades_of_that_exam() {
    recordGrade(new BigDecimal("11.00"), null);

    var response =
        restTemplate.exchange(
            "/grades?examId=" + examId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders),
            GradeResponse[].class);

    assertEquals(200, response.getStatusCode().value());
    assertEquals(1, response.getBody().length);
  }

  @Test
  void recording_a_grade_for_an_unknown_exam_should_return_404() {
    var request = new GradeRequest(studentId, UUID.randomUUID(), new BigDecimal("10.00"), null);
    var response =
        restTemplate.exchange(
            "/grades", HttpMethod.POST, new HttpEntity<>(request, teacherHeaders), Object.class);
    assertEquals(404, response.getStatusCode().value());
  }

  private UUID recordGrade(BigDecimal value, String reason) {
    var request = new GradeRequest(studentId, examId, value, reason);
    var response =
        restTemplate.exchange(
            "/grades",
            HttpMethod.POST,
            new HttpEntity<>(request, teacherHeaders),
            GradeResponse.class);
    assertEquals(201, response.getStatusCode().value());
    return response.getBody().id();
  }

  private org.springframework.http.ResponseEntity<Object> recordGradeRaw(
      BigDecimal value, String reason) {
    var request = new GradeRequest(studentId, examId, value, reason);
    return restTemplate.exchange(
        "/grades", HttpMethod.POST, new HttpEntity<>(request, teacherHeaders), Object.class);
  }
}
