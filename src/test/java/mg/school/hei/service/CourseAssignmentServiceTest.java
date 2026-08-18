package mg.school.hei.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mg.school.hei.endpoint.rest.controller.dto.CourseAssignmentRequest;
import mg.school.hei.mapper.*;
import mg.school.hei.model.UserRole;
import mg.school.hei.repository.*;
import mg.school.hei.repository.model.*;
import mg.school.hei.security.model.Principal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CourseAssignmentServiceTest {

  private final CourseAssignmentRepository courseAssignmentRepository =
          mock(CourseAssignmentRepository.class);
  private final CourseRepository courseRepository = mock(CourseRepository.class);
  private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
  private final AppGroupRepository appGroupRepository = mock(AppGroupRepository.class);
  private final ExamRepository examRepository = mock(ExamRepository.class);
  private final CourseAssignmentMapper courseAssignmentMapper =
          new CourseAssignmentMapper(new CourseMapper(), new AppUserMapper(), new AppGroupMapper());

  private final CourseAssignmentService service =
          new CourseAssignmentService(
                  courseAssignmentRepository,
                  courseRepository,
                  appUserRepository,
                  appGroupRepository,
                  examRepository,
                  courseAssignmentMapper);

  private void authenticateAs(UUID userId, UserRole role) {
    Principal principal = Principal.builder().userId(userId).role(role).build();
    var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  @BeforeEach
  void setUpSecurityContext() {
    authenticateAs(UUID.randomUUID(), UserRole.ADMIN);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void create_should_reject_an_unknown_course() {
    var courseId = UUID.randomUUID();
    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    var request = new CourseAssignmentRequest(courseId, UUID.randomUUID(), UUID.randomUUID(), 2024);

    assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(java.util.NoSuchElementException.class);
  }

  @Test
  void create_should_reject_a_duplicate_assignment_for_the_same_year() {
    var courseId = UUID.randomUUID();
    var teacherId = UUID.randomUUID();
    var groupId = UUID.randomUUID();
    var course = JCourse.builder().id(courseId).build();
    var teacher = JAppUser.builder().id(teacherId).build();
    var group = JAppGroup.builder().id(groupId).build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(appUserRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
    when(appGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

    var existing =
            JCourseAssignment.builder()
                    .course(course)
                    .teacher(teacher)
                    .group(group)
                    .academicYear(2024)
                    .build();
    when(courseAssignmentRepository.findByAcademicYear(2024)).thenReturn(List.of(existing));

    var request = new CourseAssignmentRequest(courseId, teacherId, groupId, 2024);

    assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalArgumentException.class);
    verify(courseAssignmentRepository, never()).save(any());
  }

  @Test
  void delete_should_reject_when_assignment_has_exams() {
    var id = UUID.randomUUID();
    when(courseAssignmentRepository.findById(id))
            .thenReturn(Optional.of(JCourseAssignment.builder().id(id).build()));
    when(examRepository.findByAssignmentId(id)).thenReturn(List.of(new JExam()));

    assertThatThrownBy(() -> service.delete(id)).isInstanceOf(IllegalStateException.class);
    verify(courseAssignmentRepository, never()).deleteById(any());
  }

  @Test
  void delete_should_succeed_when_assignment_has_no_exams() {
    var id = UUID.randomUUID();
    when(courseAssignmentRepository.findById(id))
            .thenReturn(Optional.of(JCourseAssignment.builder().id(id).build()));
    when(examRepository.findByAssignmentId(id)).thenReturn(List.of());

    service.delete(id);

    verify(courseAssignmentRepository).deleteById(id);
  }

  @Test
  void list_as_admin_should_return_all_assignments() {
    var teacher1 = JAppUser.builder().id(UUID.randomUUID()).build();
    var teacher2 = JAppUser.builder().id(UUID.randomUUID()).build();
    var assignment1 =
            JCourseAssignment.builder()
                    .id(UUID.randomUUID())
                    .course(JCourse.builder().id(UUID.randomUUID()).build())
                    .teacher(teacher1)
                    .group(JAppGroup.builder().id(UUID.randomUUID()).build())
                    .academicYear(2024)
                    .build();
    var assignment2 =
            JCourseAssignment.builder()
                    .id(UUID.randomUUID())
                    .course(JCourse.builder().id(UUID.randomUUID()).build())
                    .teacher(teacher2)
                    .group(JAppGroup.builder().id(UUID.randomUUID()).build())
                    .academicYear(2024)
                    .build();
    when(courseAssignmentRepository.findByAcademicYear(2024))
            .thenReturn(List.of(assignment1, assignment2));

    var result = service.list(2024);

    assertThat(result).hasSize(2);
  }

  @Test
  void list_as_teacher_should_only_return_their_own_assignments() {
    var ownTeacherId = UUID.randomUUID();
    var otherTeacherId = UUID.randomUUID();
    authenticateAs(ownTeacherId, UserRole.TEACHER);

    var ownAssignment =
            JCourseAssignment.builder()
                    .id(UUID.randomUUID())
                    .course(JCourse.builder().id(UUID.randomUUID()).build())
                    .teacher(JAppUser.builder().id(ownTeacherId).build())
                    .group(JAppGroup.builder().id(UUID.randomUUID()).build())
                    .academicYear(2024)
                    .build();
    var otherAssignment =
            JCourseAssignment.builder()
                    .id(UUID.randomUUID())
                    .course(JCourse.builder().id(UUID.randomUUID()).build())
                    .teacher(JAppUser.builder().id(otherTeacherId).build())
                    .group(JAppGroup.builder().id(UUID.randomUUID()).build())
                    .academicYear(2024)
                    .build();
    when(courseAssignmentRepository.findByAcademicYear(2024))
            .thenReturn(List.of(ownAssignment, otherAssignment));

    var result = service.list(2024);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).teacherId()).isEqualTo(ownTeacherId);
  }
}