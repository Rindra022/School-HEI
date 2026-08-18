package mg.school.hei.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import mg.school.hei.conf.FacadeIT;
import mg.school.hei.conf.RestTemplateTestConfig;
import mg.school.hei.endpoint.rest.controller.dto.CourseAssignmentRequest;
import mg.school.hei.endpoint.rest.controller.dto.CourseAssignmentResponse;
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
class CourseAssignmentControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private AppUserRepository appUserRepository;
  @Autowired private AppGroupRepository appGroupRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private CourseAssignmentRepository courseAssignmentRepository;
  @Autowired private JwtService jwtService;

  private HttpHeaders adminHeaders;
  private HttpHeaders studentHeaders;
  private UUID courseId;
  private UUID teacherId;
  private UUID groupId;

  @BeforeEach
  void setUp() {
    var admin =
        appUserRepository.save(
            JAppUser.builder()
                .firstName("Admin")
                .lastName("Assign")
                .email("ca-admin-" + UUID.randomUUID() + "@example.com")
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
                .lastName("Assign")
                .email("ca-student-" + UUID.randomUUID() + "@example.com")
                .password("hashed")
                .role(UserRole.STUDENT)
                .createdAt(Instant.now())
                .build());
    studentHeaders = new HttpHeaders();
    studentHeaders.setBearerAuth(jwtService.generateToken(student.getId(), student.getRole()));

    teacherId =
        appUserRepository
            .save(
                JAppUser.builder()
                    .firstName("T")
                    .lastName("Cher")
                    .email("ca-teacher-" + UUID.randomUUID() + "@example.com")
                    .password("hashed")
                    .role(UserRole.TEACHER)
                    .createdAt(Instant.now())
                    .build())
            .getId();
    groupId =
        appGroupRepository
            .save(JAppGroup.builder().ref("K1-" + UUID.randomUUID()).track(Track.EL).build())
            .getId();
    courseId =
        courseRepository
            .save(
                JCourse.builder()
                    .ref("PROG4-" + UUID.randomUUID())
                    .title("Qualite")
                    .credits(6)
                    .build())
            .getId();
  }

  @AfterEach
  void tearDown() {
    courseAssignmentRepository.deleteAll();
    courseRepository.deleteAll();
    appGroupRepository.deleteAll();
    appUserRepository.deleteAll();
  }

  @Test
  void creating_an_assignment_as_admin_should_return_201() {
    var response = createAssignmentRaw(2024, adminHeaders);
    assertEquals(201, response.getStatusCode().value());
  }

  @Test
  void creating_an_assignment_as_student_should_return_403() {
    var response = createAssignmentRaw(2024, studentHeaders);
    assertEquals(403, response.getStatusCode().value());
  }

  @Test
  void creating_a_duplicate_assignment_should_return_400() {
    createAssignmentRaw(2024, adminHeaders);
    var response = createAssignmentRaw(2024, adminHeaders);
    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  void listing_assignments_filtered_by_year_should_return_only_matching_ones() {
    createAssignmentRaw(2024, adminHeaders);

    var response =
        restTemplate.exchange(
            "/course-assignments?academicYear=2025",
            HttpMethod.GET,
            new HttpEntity<>(adminHeaders),
            CourseAssignmentResponse[].class);

    assertEquals(200, response.getStatusCode().value());
    assertEquals(0, response.getBody().length);
  }

  @Test
  void listing_assignments_as_teacher_should_only_return_their_own() {
    createAssignmentRaw(2028, adminHeaders);

    var otherTeacher =
        appUserRepository.save(
            JAppUser.builder()
                .firstName("Other")
                .lastName("Teacher")
                .email("ca-other-teacher-" + UUID.randomUUID() + "@example.com")
                .password("hashed")
                .role(UserRole.TEACHER)
                .createdAt(Instant.now())
                .build());
    var otherCourse =
        courseRepository.save(
            JCourse.builder().ref("OTH-" + UUID.randomUUID()).title("Other").credits(3).build());
    var otherGroup =
        appGroupRepository.save(
            JAppGroup.builder().ref("K9-" + UUID.randomUUID()).track(Track.TN).build());
    courseAssignmentRepository.save(
        JCourseAssignment.builder()
            .course(otherCourse)
            .teacher(JAppUser.builder().id(otherTeacher.getId()).build())
            .group(otherGroup)
            .academicYear(2028)
            .build());

    var ownTeacherHeaders = new HttpHeaders();
    ownTeacherHeaders.setBearerAuth(jwtService.generateToken(teacherId, UserRole.TEACHER));

    var response =
        restTemplate.exchange(
            "/course-assignments?academicYear=2028",
            HttpMethod.GET,
            new HttpEntity<>(ownTeacherHeaders),
            CourseAssignmentResponse[].class);

    assertEquals(200, response.getStatusCode().value());
    assertEquals(1, response.getBody().length);
    assertEquals(teacherId, response.getBody()[0].teacherId());
  }

  @Test
  void deleting_an_assignment_without_exams_as_admin_should_return_204() {
    var created = createAssignmentRaw(2026, adminHeaders);
    var id = ((Map<?, ?>) created.getBody()).get("id");

    var response =
        restTemplate.exchange(
            "/course-assignments/" + id,
            HttpMethod.DELETE,
            new HttpEntity<>(adminHeaders),
            Void.class);

    assertEquals(204, response.getStatusCode().value());
  }

  @Test
  void deleting_an_assignment_as_student_should_return_403() {
    var created = createAssignmentRaw(2027, adminHeaders);
    var id = ((Map<?, ?>) created.getBody()).get("id");

    var response =
        restTemplate.exchange(
            "/course-assignments/" + id,
            HttpMethod.DELETE,
            new HttpEntity<>(studentHeaders),
            Object.class);

    assertEquals(403, response.getStatusCode().value());
  }

  private org.springframework.http.ResponseEntity<Object> createAssignmentRaw(
      int academicYear, HttpHeaders headers) {
    var request = new CourseAssignmentRequest(courseId, teacherId, groupId, academicYear);
    return restTemplate.exchange(
        "/course-assignments", HttpMethod.POST, new HttpEntity<>(request, headers), Object.class);
  }
}
