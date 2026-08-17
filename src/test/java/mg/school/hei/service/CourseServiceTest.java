package mg.school.hei.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mg.school.hei.endpoint.rest.controller.dto.CourseRequest;
import mg.school.hei.mapper.CourseMapper;
import mg.school.hei.repository.CourseAssignmentRepository;
import mg.school.hei.repository.CourseRepository;
import mg.school.hei.repository.model.JCourse;
import mg.school.hei.repository.model.JCourseAssignment;
import org.junit.jupiter.api.Test;

class CourseServiceTest {

    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final CourseAssignmentRepository courseAssignmentRepository =
            mock(CourseAssignmentRepository.class);
    private final CourseMapper courseMapper = new CourseMapper();

    private final CourseService service =
            new CourseService(courseRepository, courseAssignmentRepository, courseMapper);

    @Test
    void create_should_save_and_return_the_course() {
        when(courseRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0, JCourse.class).toBuilder().id(UUID.randomUUID()).build());

        var response = service.create(new CourseRequest("PROG4", "Qualite", 6));

        assertThat(response.ref()).isEqualTo("PROG4");
        assertThat(response.credits()).isEqualTo(6);
    }

    @Test
    void get_should_throw_when_course_is_unknown() {
        var id = UUID.randomUUID();
        when(courseRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void delete_should_reject_when_course_has_assignments() {
        var id = UUID.randomUUID();
        when(courseRepository.findById(id))
                .thenReturn(Optional.of(JCourse.builder().id(id).ref("PROG4").title("Q").credits(6).build()));
        var assignment =
                JCourseAssignment.builder().course(JCourse.builder().id(id).build()).build();
        when(courseAssignmentRepository.findAll()).thenReturn(List.of(assignment));

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(IllegalStateException.class);
        verify(courseRepository, never()).deleteById(any());
    }

    @Test
    void delete_should_succeed_when_course_has_no_assignments() {
        var id = UUID.randomUUID();
        when(courseRepository.findById(id))
                .thenReturn(Optional.of(JCourse.builder().id(id).ref("PROG4").title("Q").credits(6).build()));
        when(courseAssignmentRepository.findAll()).thenReturn(List.of());

        service.delete(id);

        verify(courseRepository).deleteById(id);
    }

    @Test
    void update_should_replace_all_fields() {
        var id = UUID.randomUUID();
        var entity = JCourse.builder().id(id).ref("OLD").title("Old title").credits(3).build();
        when(courseRepository.findById(id)).thenReturn(Optional.of(entity));
        when(courseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.update(id, new CourseRequest("NEW", "New title", 5));

        assertThat(response.ref()).isEqualTo("NEW");
        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.credits()).isEqualTo(5);
    }
}