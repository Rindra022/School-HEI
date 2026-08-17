package mg.school.hei.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import mg.school.hei.endpoint.rest.controller.dto.FullTranscriptResponse;
import mg.school.hei.endpoint.rest.controller.dto.TranscriptCourseLine;
import mg.school.hei.endpoint.rest.controller.dto.TranscriptResponse;
import mg.school.hei.model.Track;
import mg.school.hei.repository.*;
import mg.school.hei.repository.model.*;
import org.junit.jupiter.api.Test;

class GraduateServiceTest {

  private final PromotionRepository promotionRepository = mock(PromotionRepository.class);
  private final StudentRepository studentRepository = mock(StudentRepository.class);
  private final GroupMembershipRepository groupMembershipRepository =
      mock(GroupMembershipRepository.class);
  private final TranscriptService transcriptService = mock(TranscriptService.class);

  private final GraduateService service =
      new GraduateService(
          promotionRepository, studentRepository, groupMembershipRepository, transcriptService);

  private JStudent studentWith(UUID id, String std) {
    var appUser = JAppUser.builder().firstName("Jean").lastName("Rakoto").build();
    return JStudent.builder().id(id).std(std).appUser(appUser).build();
  }

  private FullTranscriptResponse fullTranscript(
      UUID studentId, boolean complete, Double average, double... courseAverages) {
    var lines =
        java.util.Arrays.stream(courseAverages)
            .mapToObj(a -> new TranscriptCourseLine("REF", "Title", a, 6, true))
            .toList();
    var year = new TranscriptResponse(studentId, "STD", 2024, lines, average, 6, complete);
    return new FullTranscriptResponse(studentId, "STD", List.of(year), average, 6, complete);
  }

  @Test
  void student_with_all_courses_above_10_should_be_listed_as_graduate() {
    var promotionId = UUID.randomUUID();
    var studentId = UUID.randomUUID();
    var groupId = UUID.randomUUID();

    when(promotionRepository.existsById(promotionId)).thenReturn(true);
    when(studentRepository.findByPromotionId(promotionId))
        .thenReturn(List.of(studentWith(studentId, "STD24001")));
    when(transcriptService.getFullTranscript(studentId))
        .thenReturn(fullTranscript(studentId, true, 13.5, 12.0, 15.0));
    when(groupMembershipRepository.findByStudentIdOrderByStartDateAsc(studentId))
        .thenReturn(
            List.of(
                JGroupMembership.builder()
                    .startDate(LocalDate.of(2024, 9, 1))
                    .group(JAppGroup.builder().id(groupId).track(Track.EL).build())
                    .build()));

    var result = service.listGraduates(promotionId, null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).std()).isEqualTo("STD24001");
    assertThat(result.get(0).track()).isEqualTo(Track.EL);
    assertThat(result.get(0).rank()).isEqualTo(1);
  }

  @Test
  void student_with_a_course_below_10_should_not_be_listed() {
    var promotionId = UUID.randomUUID();
    var studentId = UUID.randomUUID();

    when(promotionRepository.existsById(promotionId)).thenReturn(true);
    when(studentRepository.findByPromotionId(promotionId))
        .thenReturn(List.of(studentWith(studentId, "STD24002")));
    when(transcriptService.getFullTranscript(studentId))
        .thenReturn(fullTranscript(studentId, true, 9.0, 8.0, 15.0));

    var result = service.listGraduates(promotionId, null);

    assertThat(result).isEmpty();
  }

  @Test
  void incomplete_transcript_should_not_be_listed() {
    var promotionId = UUID.randomUUID();
    var studentId = UUID.randomUUID();

    when(promotionRepository.existsById(promotionId)).thenReturn(true);
    when(studentRepository.findByPromotionId(promotionId))
        .thenReturn(List.of(studentWith(studentId, "STD24003")));
    when(transcriptService.getFullTranscript(studentId))
        .thenReturn(fullTranscript(studentId, false, 13.0, 13.0));

    var result = service.listGraduates(promotionId, null);

    assertThat(result).isEmpty();
  }

  @Test
  void track_filter_should_exclude_graduates_from_other_track() {
    var promotionId = UUID.randomUUID();
    var studentId = UUID.randomUUID();

    when(promotionRepository.existsById(promotionId)).thenReturn(true);
    when(studentRepository.findByPromotionId(promotionId))
        .thenReturn(List.of(studentWith(studentId, "STD24004")));
    when(transcriptService.getFullTranscript(studentId))
        .thenReturn(fullTranscript(studentId, true, 14.0, 14.0));
    when(groupMembershipRepository.findByStudentIdOrderByStartDateAsc(studentId))
        .thenReturn(
            List.of(
                JGroupMembership.builder()
                    .startDate(LocalDate.of(2024, 9, 1))
                    .group(JAppGroup.builder().id(UUID.randomUUID()).track(Track.TN).build())
                    .build()));

    var result = service.listGraduates(promotionId, Track.EL);

    assertThat(result).isEmpty();
  }

  @Test
  void unknown_promotion_should_throw() {
    var promotionId = UUID.randomUUID();
    when(promotionRepository.existsById(promotionId)).thenReturn(false);

    assertThatThrownBy(() -> service.listGraduates(promotionId, null))
        .isInstanceOf(java.util.NoSuchElementException.class);
  }

  @Test
  void track_should_be_taken_from_the_most_recent_group_membership() {
    var promotionId = UUID.randomUUID();
    var studentId = UUID.randomUUID();

    when(promotionRepository.existsById(promotionId)).thenReturn(true);
    when(studentRepository.findByPromotionId(promotionId))
        .thenReturn(List.of(studentWith(studentId, "STD24005")));
    when(transcriptService.getFullTranscript(studentId))
        .thenReturn(fullTranscript(studentId, true, 12.0, 12.0));
    when(groupMembershipRepository.findByStudentIdOrderByStartDateAsc(studentId))
        .thenReturn(
            List.of(
                JGroupMembership.builder()
                    .startDate(LocalDate.of(2022, 9, 1))
                    .endDate(LocalDate.of(2023, 8, 31))
                    .group(JAppGroup.builder().id(UUID.randomUUID()).track(Track.COMMUN).build())
                    .build(),
                JGroupMembership.builder()
                    .startDate(LocalDate.of(2023, 9, 1))
                    .group(JAppGroup.builder().id(UUID.randomUUID()).track(Track.TN).build())
                    .build()));

    var result = service.listGraduates(promotionId, null);

    assertThat(result.get(0).track()).isEqualTo(Track.TN);
  }
}
