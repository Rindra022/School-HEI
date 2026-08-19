package mg.school.hei.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import mg.school.hei.conf.FacadeIT;
import mg.school.hei.conf.RestTemplateTestConfig;
import mg.school.hei.endpoint.rest.controller.dto.*;
import mg.school.hei.file.bucket.BucketComponent;
import mg.school.hei.model.Track;
import mg.school.hei.model.UserRole;
import mg.school.hei.repository.*;
import mg.school.hei.repository.model.*;
import mg.school.hei.security.jwt.JwtService;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(RestTemplateTestConfig.class)
class GraduateExportControllerIT extends FacadeIT {

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
  @MockBean private BucketComponent bucketComponent;

  private final Map<String, byte[]> s3Storage = new HashMap<>();
  private HttpHeaders adminHeaders;
  private HttpHeaders studentHeaders;
  private UUID promotionId;

  @BeforeEach
  void setUp() {
    s3Storage.clear();

    lenient()
        .when(bucketComponent.upload(any(File.class), anyString()))
        .thenAnswer(
            inv -> {
              File file = inv.getArgument(0);
              String key = inv.getArgument(1);
              s3Storage.put(key, Files.readAllBytes(file.toPath()));
              return null;
            });
    lenient()
        .when(bucketComponent.presign(anyString(), any(Duration.class)))
        .thenAnswer(
            inv -> {
              String key = inv.getArgument(0);
              return new URL("https://dummy-bucket.s3.eu-west-3.amazonaws.com/" + key);
            });
    lenient()
        .when(bucketComponent.download(anyString()))
        .thenAnswer(
            inv -> {
              String key = inv.getArgument(0);
              byte[] data = s3Storage.get(key);
              File tmp = File.createTempFile("dl-", ".xlsx");
              Files.write(tmp.toPath(), data);
              return tmp;
            });

    var promotion = promotionRepository.save(JPromotion.builder().year(2024).build());
    promotionId = promotion.getId();

    var admin =
        appUserRepository.save(
            JAppUser.builder()
                .firstName("Admin")
                .lastName("Export")
                .email("export-admin-" + UUID.randomUUID() + "@example.com")
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
                .lastName("Export")
                .email("export-student-" + UUID.randomUUID() + "@example.com")
                .password("hashed")
                .role(UserRole.STUDENT)
                .createdAt(Instant.now())
                .build());
    studentHeaders = new HttpHeaders();
    studentHeaders.setBearerAuth(jwtService.generateToken(student.getId(), student.getRole()));

    var graduatingStudent =
        appUserRepository.save(
            JAppUser.builder()
                .firstName("Grad")
                .lastName("Uate")
                .email("export-grad-" + UUID.randomUUID() + "@example.com")
                .password("hashed")
                .role(UserRole.STUDENT)
                .createdAt(Instant.now())
                .build());
    studentRepository.save(
        JStudent.builder()
            .id(graduatingStudent.getId())
            .std("STD24080")
            .promotion(promotion)
            .build());

    var teacher =
        appUserRepository.save(
            JAppUser.builder()
                .firstName("Teacher")
                .lastName("Export")
                .email("export-teacher-" + UUID.randomUUID() + "@example.com")
                .password("hashed")
                .role(UserRole.TEACHER)
                .createdAt(Instant.now())
                .build());

    var group =
        appGroupRepository.save(
            JAppGroup.builder().ref("K1-" + UUID.randomUUID()).track(Track.EL).build());
    var course =
        courseRepository.save(
            JCourse.builder()
                .ref("PROG4-" + UUID.randomUUID())
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

    restTemplate.exchange(
        "/group-memberships",
        HttpMethod.POST,
        new HttpEntity<>(
            new GroupMembershipRequest(
                graduatingStudent.getId(), group.getId(), LocalDate.of(2024, 9, 1)),
            adminHeaders),
        GroupMembershipResponse.class);

    var teacherHeaders = new HttpHeaders();
    teacherHeaders.setBearerAuth(jwtService.generateToken(teacher.getId(), UserRole.TEACHER));

    var examResponse =
        restTemplate.exchange(
            "/exams",
            HttpMethod.POST,
            new HttpEntity<>(
                new ExamRequest(assignment.getId(), Instant.now(), BigDecimal.ONE), teacherHeaders),
            ExamResponse.class);

    restTemplate.exchange(
        "/grades",
        HttpMethod.POST,
        new HttpEntity<>(
            new GradeRequest(
                graduatingStudent.getId(),
                examResponse.getBody().id(),
                new BigDecimal("17.00"),
                null),
            teacherHeaders),
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
    appUserRepository.deleteAll();
    promotionRepository.deleteAll();
  }

  @Test
  void export_as_admin_should_return_200_with_a_downloadable_xlsx_containing_the_graduate()
      throws Exception {
    var response = exportRaw(adminHeaders);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());

    @SuppressWarnings("unchecked")
    var body = (Map<String, String>) response.getBody();
    var location = body.get("url");
    assertNotNull(location);

    String bucketKey = URI.create(location).getPath().substring(1);
    var downloaded = bucketComponent.download(bucketKey);

    try (XSSFWorkbook workbook = new XSSFWorkbook(downloaded)) {
      var elSheet = workbook.getSheet("EL");
      assertNotNull(elSheet);

      Row header = elSheet.getRow(0);
      assertEquals("Rang", header.getCell(0).getStringCellValue());
      assertEquals("STD", header.getCell(1).getStringCellValue());

      Row graduateRow = elSheet.getRow(1);
      assertNotNull(graduateRow);
      assertEquals("STD24080", graduateRow.getCell(1).getStringCellValue());
      assertEquals(17.0, graduateRow.getCell(4).getNumericCellValue());

      var tnSheet = workbook.getSheet("TN");
      assertNotNull(tnSheet);
      assertEquals(0, tnSheet.getLastRowNum());
    }
  }

  @Test
  void export_as_student_should_return_403() {
    var response = exportRaw(studentHeaders);
    assertEquals(403, response.getStatusCode().value());
  }

  @Test
  void export_for_an_unknown_promotion_should_return_404() {
    var noRedirectTemplate = createNoRedirectRestTemplate();
    String url =
        restTemplate.getRootUri() + "/promotions/" + UUID.randomUUID() + "/graduates/export";

    var response =
        noRedirectTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(adminHeaders), Object.class);

    assertEquals(404, response.getStatusCode().value());
  }

  private RestTemplate createNoRedirectRestTemplate() {
    var factory =
        new HttpComponentsClientHttpRequestFactory(
            HttpClients.custom().disableRedirectHandling().build());
    var template = new RestTemplate(factory);
    template.setErrorHandler(
        new ResponseErrorHandler() {
          @Override
          public boolean hasError(ClientHttpResponse response) {
            return false;
          }

          @Override
          public void handleError(ClientHttpResponse response) {}
        });
    return template;
  }

  private ResponseEntity<Map> exportRaw(HttpHeaders headers) {
    var noRedirectTemplate = createNoRedirectRestTemplate();
    String url = restTemplate.getRootUri() + "/promotions/" + promotionId + "/graduates/export";
    return noRedirectTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
  }
}
