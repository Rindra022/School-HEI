package mg.school.hei.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mg.school.hei.endpoint.event.EventProducer;
import mg.school.hei.endpoint.event.model.TranscriptPdfRequested;
import mg.school.hei.repository.*;
import mg.school.hei.repository.model.*;
import org.junit.jupiter.api.Test;

class TranscriptServiceTest {

  private final StudentRepository studentRepository = mock(StudentRepository.class);
  private final GroupMembershipRepository groupMembershipRepository =
      mock(GroupMembershipRepository.class);
  private final CourseAssignmentRepository courseAssignmentRepository =
      mock(CourseAssignmentRepository.class);
  private final ExamRepository examRepository = mock(ExamRepository.class);
  private final GradeRepository gradeRepository = mock(GradeRepository.class);
  private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
  private final EventProducer<TranscriptPdfRequested> eventProducer = mock(EventProducer.class);
  private final TranscriptService service =
      new TranscriptService(
          studentRepository,
          groupMembershipRepository,
          courseAssignmentRepository,
          examRepository,
          gradeRepository,
          appUserRepository,
          eventProducer);

  @Test
  void course_should_be_marked_incomplete_when_a_grade_is_missing() {
    var studentId = UUID.randomUUID();
    var groupId = UUID.randomUUID();
    var assignmentId = UUID.randomUUID();

    when(studentRepository.findById(studentId))
        .thenReturn(Optional.of(JStudent.builder().std("STD24001").build()));
    when(groupMembershipRepository.findByStudentIdOrderByStartDateAsc(studentId))
        .thenReturn(
            List.of(
                JGroupMembership.builder()
                    .startDate(LocalDate.of(2024, 9, 1))
                    .endDate(null)
                    .group(JAppGroup.builder().id(groupId).build())
                    .build()));

    var course = JCourse.builder().ref("PROG4").title("Qualite").credits(6).build();
    var assignment =
        JCourseAssignment.builder()
            .id(assignmentId)
            .course(course)
            .group(JAppGroup.builder().id(groupId).build())
            .build();
    when(courseAssignmentRepository.findByAcademicYear(2024)).thenReturn(List.of(assignment));

    var exam1 = JExam.builder().id(UUID.randomUUID()).coefficient(new BigDecimal("0.50")).build();
    var exam2 = JExam.builder().id(UUID.randomUUID()).coefficient(new BigDecimal("0.50")).build();
    when(examRepository.findByAssignmentId(assignmentId)).thenReturn(List.of(exam1, exam2));

    when(gradeRepository.findByStudentIdAndExamIdAndCurrentTrue(studentId, exam1.getId()))
        .thenReturn(Optional.of(JGrade.builder().value(new BigDecimal("12.00")).build()));
    when(gradeRepository.findByStudentIdAndExamIdAndCurrentTrue(studentId, exam2.getId()))
        .thenReturn(Optional.empty());

    var response = service.getTranscript(studentId, 2024);

    assertThat(response.complete()).isFalse();
    assertThat(response.courses()).hasSize(1);
    assertThat(response.courses().get(0).complete()).isFalse();
    assertThat(response.courses().get(0).average()).isEqualTo(12.00);
  }

  @Test
  void course_should_be_complete_and_weighted_correctly_when_all_grades_present() {
    var studentId = UUID.randomUUID();
    var groupId = UUID.randomUUID();
    var assignmentId = UUID.randomUUID();

    when(studentRepository.findById(studentId))
        .thenReturn(Optional.of(JStudent.builder().std("STD24002").build()));
    when(groupMembershipRepository.findByStudentIdOrderByStartDateAsc(studentId))
        .thenReturn(
            List.of(
                JGroupMembership.builder()
                    .startDate(LocalDate.of(2024, 9, 1))
                    .endDate(null)
                    .group(JAppGroup.builder().id(groupId).build())
                    .build()));

    var course = JCourse.builder().ref("PROG4").title("Qualite").credits(6).build();
    var assignment =
        JCourseAssignment.builder()
            .id(assignmentId)
            .course(course)
            .group(JAppGroup.builder().id(groupId).build())
            .build();
    when(courseAssignmentRepository.findByAcademicYear(2024)).thenReturn(List.of(assignment));

    var exam1 = JExam.builder().id(UUID.randomUUID()).coefficient(new BigDecimal("0.30")).build();
    var exam2 = JExam.builder().id(UUID.randomUUID()).coefficient(new BigDecimal("0.70")).build();
    when(examRepository.findByAssignmentId(assignmentId)).thenReturn(List.of(exam1, exam2));

    when(gradeRepository.findByStudentIdAndExamIdAndCurrentTrue(studentId, exam1.getId()))
        .thenReturn(Optional.of(JGrade.builder().value(new BigDecimal("10.00")).build()));
    when(gradeRepository.findByStudentIdAndExamIdAndCurrentTrue(studentId, exam2.getId()))
        .thenReturn(Optional.of(JGrade.builder().value(new BigDecimal("15.00")).build()));

    var response = service.getTranscript(studentId, 2024);

    assertThat(response.complete()).isTrue();
    assertThat(response.courses().get(0).average()).isEqualTo(13.50); // 10*0.3 + 15*0.7
    assertThat(response.generalAverage()).isEqualTo(13.50);
    assertThat(response.totalCredits()).isEqualTo(6);
  }

  @Test
  void a_course_from_a_group_the_student_was_never_in_should_not_appear() {
    var studentId = UUID.randomUUID();
    var actualGroupId = UUID.randomUUID();
    var otherGroupId = UUID.randomUUID();

    when(studentRepository.findById(studentId))
        .thenReturn(Optional.of(JStudent.builder().std("STD24003").build()));
    when(groupMembershipRepository.findByStudentIdOrderByStartDateAsc(studentId))
        .thenReturn(
            List.of(
                JGroupMembership.builder()
                    .startDate(LocalDate.of(2024, 9, 1))
                    .endDate(null)
                    .group(JAppGroup.builder().id(actualGroupId).build())
                    .build()));

    var tn1Assignment =
        JCourseAssignment.builder()
            .id(UUID.randomUUID())
            .course(JCourse.builder().ref("TN1").title("Marketing").credits(3).build())
            .group(JAppGroup.builder().id(otherGroupId).build())
            .build();
    when(courseAssignmentRepository.findByAcademicYear(2024)).thenReturn(List.of(tn1Assignment));

    var response = service.getTranscript(studentId, 2024);

    assertThat(response.courses()).isEmpty();
  }

  @Test
  void full_transcript_should_aggregate_every_year_the_student_was_enrolled() {
    var studentId = UUID.randomUUID();
    when(studentRepository.findById(studentId))
        .thenReturn(Optional.of(JStudent.builder().std("STD24004").build()));
    when(groupMembershipRepository.findByStudentIdOrderByStartDateAsc(studentId))
        .thenReturn(
            List.of(
                JGroupMembership.builder()
                    .startDate(LocalDate.of(2024, 9, 1))
                    .endDate(LocalDate.of(2025, 8, 31))
                    .group(JAppGroup.builder().id(UUID.randomUUID()).build())
                    .build()));
    when(courseAssignmentRepository.findByAcademicYear(anyInt())).thenReturn(List.of());

    var response = service.getFullTranscript(studentId);

    assertThat(response.years())
        .extracting(mg.school.hei.endpoint.rest.controller.dto.TranscriptResponse::academicYear)
        .containsExactly(2024, 2025);
  }

  @Test
  void requestTranscriptPdf_should_publish_event_with_student_email() {
    var studentId = UUID.randomUUID();
    var user =
        mg.school.hei.repository.model.JAppUser.builder()
            .id(studentId)
            .email("alice@hei.school")
            .build();
    when(appUserRepository.findById(studentId)).thenReturn(Optional.of(user));

    service.requestTranscriptPdf(studentId);

    var captor = org.mockito.ArgumentCaptor.forClass(java.util.List.class);
    verify(eventProducer).accept(captor.capture());
    var events = (java.util.List<TranscriptPdfRequested>) captor.getValue();
    assertThat(events).hasSize(1);
    assertThat(events.get(0).getStudentId()).isEqualTo(studentId.toString());
    assertThat(events.get(0).getRecipientEmail()).isEqualTo("alice@hei.school");
  }

  @Test
  void requestTranscriptPdf_should_reject_an_unknown_student() {
    var studentId = UUID.randomUUID();
    when(appUserRepository.findById(studentId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.requestTranscriptPdf(studentId))
        .isInstanceOf(java.util.NoSuchElementException.class);
    verify(eventProducer, never()).accept(any());
  }
}
