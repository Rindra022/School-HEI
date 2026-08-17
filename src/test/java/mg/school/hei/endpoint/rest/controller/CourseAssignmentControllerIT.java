package mg.school.hei.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.time.Instant;
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
class CourseAssignmentControllerIT extends FacadeIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private PromotionRepository promotionRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private AppGroupRepository appGroupRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CourseAssignmentRepository courseAssignmentRepository;
    @Autowired private StudentRepository studentRepository;

    private HttpHeaders authHeaders;
    private UUID courseId;
    private UUID teacherId;
    private UUID groupId;

    @BeforeEach
    void setUp() {
        var promotion = promotionRepository.save(JPromotion.builder().year(2024 + (int)(Math.random() * 1000)).build());
        String email = "ca-auth-" + UUID.randomUUID() + "@example.com";
        restTemplate.postForEntity(
                "/register",
                new RegisterRequest("CA", "Tester", null, email, "password123", null, promotion.getId()),
                Void.class);
        var login =
                restTemplate.postForEntity(
                        "/login", new LoginRequest(email, "password123"), AuthResponse.class);
        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(login.getBody().token());

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
                        .save(JAppGroup.builder().ref("K1-" + UUID.randomUUID().toString().substring(0, 8)).track(Track.EL).build())
                        .getId();
        courseId =
                courseRepository
                        .save(JCourse.builder().ref("PROG-" + UUID.randomUUID().toString().substring(0, 8)).title("Qualite").credits(6).build())
                        .getId();
    }

    @AfterEach
    void tearDown() {
        courseAssignmentRepository.deleteAll();
        courseRepository.deleteAll();
        appGroupRepository.deleteAll();
        studentRepository.deleteAll();
        appUserRepository.deleteAll();
        promotionRepository.deleteAll();
    }

    @Test
    void creating_an_assignment_should_return_201() {
        var response = createAssignmentRaw(2024);
        assertEquals(201, response.getStatusCode().value());
    }

    @Test
    void creating_a_duplicate_assignment_should_return_400() {
        createAssignmentRaw(2024);
        var response = createAssignmentRaw(2024);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void listing_assignments_filtered_by_year_should_return_only_matching_ones() {
        createAssignmentRaw(2024);

        var response =
                restTemplate.exchange(
                        "/course-assignments?academicYear=2025",
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders),
                        CourseAssignmentResponse[].class);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(0, response.getBody().length);
    }

    @Test
    void deleting_an_assignment_without_exams_should_return_204() {
        var created = createAssignmentRaw(2026);
        var id = ((java.util.Map<?, ?>) created.getBody()).get("id");

        var response =
                restTemplate.exchange(
                        "/course-assignments/" + id, HttpMethod.DELETE, new HttpEntity<>(authHeaders), Void.class);

        assertEquals(204, response.getStatusCode().value());
    }

    private org.springframework.http.ResponseEntity<Object> createAssignmentRaw(int academicYear) {
        var request = new CourseAssignmentRequest(courseId, teacherId, groupId, academicYear);
        return restTemplate.exchange(
                "/course-assignments", HttpMethod.POST, new HttpEntity<>(request, authHeaders), Object.class);
    }
}