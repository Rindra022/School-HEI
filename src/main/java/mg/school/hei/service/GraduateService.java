package mg.school.hei.service;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.FullTranscriptResponse;
import mg.school.hei.endpoint.rest.controller.dto.GraduateResponse;
import mg.school.hei.model.Track;
import mg.school.hei.repository.GroupMembershipRepository;
import mg.school.hei.repository.PromotionRepository;
import mg.school.hei.repository.StudentRepository;
import mg.school.hei.repository.model.JStudent;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GraduateService {
  private final PromotionRepository promotionRepository;
  private final StudentRepository studentRepository;
  private final GroupMembershipRepository groupMembershipRepository;
  private final TranscriptService transcriptService;

  public List<GraduateResponse> listGraduates(UUID promotionId, Track trackFilter) {
    if (!promotionRepository.existsById(promotionId)) {
      throw new NoSuchElementException("Promotion not found");
    }

    List<JStudent> students = studentRepository.findByPromotionId(promotionId);

    List<GraduateResponse> ranked =
        students.stream()
            .map(this::toGraduateOrNull)
            .filter(g -> g != null)
            .filter(g -> trackFilter == null || g.track() == trackFilter)
            .sorted(Comparator.comparingDouble(GraduateResponse::generalAverage).reversed())
            .toList();

    return IntStream.range(0, ranked.size()).mapToObj(i -> withRank(ranked.get(i), i + 1)).toList();
  }

  private GraduateResponse toGraduateOrNull(JStudent student) {
    FullTranscriptResponse transcript = transcriptService.getFullTranscript(student.getId());

    boolean allCoursesValidated =
        transcript.complete()
            && transcript.years().stream()
                .flatMap(y -> y.courses().stream())
                .allMatch(c -> c.average() != null && c.average() >= 10.0);

    if (!allCoursesValidated || transcript.cumulativeGeneralAverage() == null) {
      return null;
    }

    Track track = lastTrack(student.getId());
    return new GraduateResponse(
        0,
        student.getStd(),
        student.getAppUser().getFirstName(),
        student.getAppUser().getLastName(),
        transcript.cumulativeGeneralAverage(),
        track);
  }

  private Track lastTrack(UUID studentId) {
    return groupMembershipRepository.findByStudentIdOrderByStartDateAsc(studentId).stream()
        .reduce((first, second) -> second)
        .map(m -> m.getGroup().getTrack())
        .orElseThrow(() -> new NoSuchElementException("Student has no group membership"));
  }

  private GraduateResponse withRank(GraduateResponse g, int rank) {
    return new GraduateResponse(
        rank, g.std(), g.firstName(), g.lastName(), g.generalAverage(), g.track());
  }
}
