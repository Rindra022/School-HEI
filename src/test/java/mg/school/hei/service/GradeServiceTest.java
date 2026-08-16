package mg.school.hei.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mg.school.hei.endpoint.rest.controller.dto.GradeRequest;
import mg.school.hei.mapper.GradeMapper;
import mg.school.hei.model.Exam;
import mg.school.hei.model.Grade;
import mg.school.hei.model.Student;
import mg.school.hei.repository.ExamRepository;
import mg.school.hei.repository.GradeRepository;
import mg.school.hei.repository.StudentRepository;
import mg.school.hei.repository.model.JExam;
import mg.school.hei.repository.model.JGrade;
import mg.school.hei.repository.model.JStudent;
import org.junit.jupiter.api.Test;

class GradeServiceTest {

  private final GradeRepository gradeRepository = mock(GradeRepository.class);
  private final StudentRepository studentRepository = mock(StudentRepository.class);
  private final ExamRepository examRepository = mock(ExamRepository.class);
  private final GradeMapper gradeMapper = mock(GradeMapper.class);

  private final GradeService service =
      new GradeService(gradeRepository, studentRepository, examRepository, gradeMapper);

  private Grade fakeModel(UUID id, BigDecimal value, boolean current, Grade previous) {
    return new Grade(
        id,
        new Student(UUID.randomUUID(), "STD24001", null),
        new Exam(UUID.randomUUID(), null, java.time.Instant.now(), BigDecimal.ONE),
        value,
        java.time.Instant.now(),
        null,
        previous,
        current);
  }

  @Test
  void record_should_reject_a_second_grade_without_a_reason() {
    var studentId = UUID.randomUUID();
    var examId = UUID.randomUUID();
    var student = JStudent.builder().id(studentId).build();
    var exam = JExam.builder().id(examId).build();

    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
    when(gradeRepository.findByStudentIdAndExamIdAndCurrentTrue(studentId, examId))
        .thenReturn(Optional.of(new JGrade()));

    var request = new GradeRequest(studentId, examId, new BigDecimal("15.00"), null);

    assertThatThrownBy(() -> service.record(request)).isInstanceOf(IllegalArgumentException.class);
    verify(gradeRepository, never()).save(any());
  }

  @Test
  void record_should_reject_a_second_grade_with_a_blank_reason() {
    var studentId = UUID.randomUUID();
    var examId = UUID.randomUUID();
    when(studentRepository.findById(studentId))
        .thenReturn(Optional.of(JStudent.builder().id(studentId).build()));
    when(examRepository.findById(examId))
        .thenReturn(Optional.of(JExam.builder().id(examId).build()));
    when(gradeRepository.findByStudentIdAndExamIdAndCurrentTrue(studentId, examId))
        .thenReturn(Optional.of(new JGrade()));

    var request = new GradeRequest(studentId, examId, new BigDecimal("15.00"), "   ");

    assertThatThrownBy(() -> service.record(request)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void record_should_close_the_previous_grade_when_amending_with_a_reason() {
    var studentId = UUID.randomUUID();
    var examId = UUID.randomUUID();
    var student = JStudent.builder().id(studentId).build();
    var exam = JExam.builder().id(examId).build();
    var previous = JGrade.builder().current(true).build();

    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
    when(gradeRepository.findByStudentIdAndExamIdAndCurrentTrue(studentId, examId))
        .thenReturn(Optional.of(previous));
    when(gradeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(gradeMapper.toModel(any()))
        .thenReturn(fakeModel(UUID.randomUUID(), new BigDecimal("14"), true, null));

    var request = new GradeRequest(studentId, examId, new BigDecimal("14.00"), "Erreur de saisie");

    service.record(request);

    assertThat(previous.isCurrent()).isFalse();
    verify(gradeRepository, times(2)).save(any());
  }

  @Test
  void list_should_use_exam_filter_when_only_examId_is_provided() {
    var examId = UUID.randomUUID();
    when(gradeRepository.findByExamIdAndCurrentTrue(examId)).thenReturn(List.of(new JGrade()));
    when(gradeMapper.toModel(any()))
        .thenReturn(fakeModel(UUID.randomUUID(), BigDecimal.TEN, true, null));

    var result = service.list(null, examId);

    assertThat(result).hasSize(1);
    verify(gradeRepository).findByExamIdAndCurrentTrue(examId);
  }

  @Test
  void list_should_return_empty_when_no_filter_is_provided() {
    var result = service.list(null, null);

    assertThat(result).isEmpty();
    verifyNoInteractions(gradeRepository, gradeMapper);
  }
}
