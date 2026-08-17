package mg.school.hei.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mg.school.hei.endpoint.rest.controller.dto.CourseAssignmentRequest;
import mg.school.hei.mapper.*;
import mg.school.hei.repository.*;
import mg.school.hei.repository.model.*;
import org.junit.jupiter.api.Test;

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
}