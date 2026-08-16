package mg.school.hei.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mg.school.hei.endpoint.rest.controller.dto.ExamRequest;
import mg.school.hei.mapper.ExamMapper;
import mg.school.hei.repository.CourseAssignmentRepository;
import mg.school.hei.repository.ExamRepository;
import mg.school.hei.repository.GradeRepository;
import mg.school.hei.repository.model.JCourseAssignment;
import mg.school.hei.repository.model.JExam;
import mg.school.hei.repository.model.JGrade;
import org.junit.jupiter.api.Test;

class ExamServiceTest {

  private final ExamRepository examRepository = mock(ExamRepository.class);
  private final CourseAssignmentRepository courseAssignmentRepository =
      mock(CourseAssignmentRepository.class);
  private final GradeRepository gradeRepository = mock(GradeRepository.class);
  private final ExamMapper examMapper = mock(ExamMapper.class);

  private final ExamService service =
      new ExamService(examRepository, courseAssignmentRepository, gradeRepository, examMapper);

  @Test
  void create_should_reject_an_unknown_assignment() {
    var assignmentId = UUID.randomUUID();
    when(courseAssignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

    var request = new ExamRequest(assignmentId, Instant.now(), new BigDecimal("0.50"));

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(java.util.NoSuchElementException.class);
  }

  @Test
  void create_should_reject_when_coefficient_sum_would_exceed_one() {
    var assignmentId = UUID.randomUUID();
    var assignment = JCourseAssignment.builder().id(assignmentId).build();

    when(courseAssignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
    when(examRepository.findByAssignmentId(assignmentId))
        .thenReturn(List.of(JExam.builder().coefficient(new BigDecimal("0.70")).build()));

    var request = new ExamRequest(assignmentId, Instant.now(), new BigDecimal("0.50"));

    assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalArgumentException.class);
    verify(examRepository, never()).save(any());
  }

  @Test
  void create_should_accept_when_coefficient_sum_equals_exactly_one() {
    var assignmentId = UUID.randomUUID();
    var assignment = JCourseAssignment.builder().id(assignmentId).build();

    when(courseAssignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
    when(examRepository.findByAssignmentId(assignmentId))
        .thenReturn(List.of(JExam.builder().coefficient(new BigDecimal("0.60")).build()));
    when(examRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(examMapper.toModel(any()))
        .thenReturn(
            new mg.school.hei.model.Exam(
                UUID.randomUUID(),
                new mg.school.hei.model.CourseAssignment(assignmentId, null, null, null, null),
                Instant.now(),
                new BigDecimal("0.40")));

    var request = new ExamRequest(assignmentId, Instant.now(), new BigDecimal("0.40"));

    var response = service.create(request);

    assertThat(response.coefficient()).isEqualByComparingTo("0.40");
    verify(examRepository).save(any());
  }

  @Test
  void delete_should_reject_when_exam_has_grades() {
    var examId = UUID.randomUUID();
    when(examRepository.existsById(examId)).thenReturn(true);
    when(gradeRepository.findByExamId(examId)).thenReturn(List.of(new JGrade()));

    assertThatThrownBy(() -> service.delete(examId)).isInstanceOf(IllegalStateException.class);
    verify(examRepository, never()).deleteById(any());
  }

  @Test
  void delete_should_reject_an_unknown_exam() {
    var examId = UUID.randomUUID();
    when(examRepository.existsById(examId)).thenReturn(false);

    assertThatThrownBy(() -> service.delete(examId))
        .isInstanceOf(java.util.NoSuchElementException.class);
  }

  @Test
  void delete_should_succeed_when_exam_has_no_grades() {
    var examId = UUID.randomUUID();
    when(examRepository.existsById(examId)).thenReturn(true);
    when(gradeRepository.findByExamId(examId)).thenReturn(List.of());

    service.delete(examId);

    verify(examRepository).deleteById(examId);
  }
}
