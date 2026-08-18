package mg.school.hei.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.event.EventProducer;
import mg.school.hei.endpoint.event.model.TranscriptPdfRequested;
import mg.school.hei.endpoint.rest.controller.dto.FullTranscriptResponse;
import mg.school.hei.endpoint.rest.controller.dto.TranscriptCourseLine;
import mg.school.hei.endpoint.rest.controller.dto.TranscriptResponse;
import mg.school.hei.repository.*;
import mg.school.hei.repository.AppUserRepository;
import mg.school.hei.repository.model.JCourseAssignment;
import mg.school.hei.repository.model.JExam;
import mg.school.hei.repository.model.JGroupMembership;
import mg.school.hei.repository.model.JStudent;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TranscriptService {
  private final StudentRepository studentRepository;
  private final GroupMembershipRepository groupMembershipRepository;
  private final CourseAssignmentRepository courseAssignmentRepository;
  private final ExamRepository examRepository;
  private final GradeRepository gradeRepository;
  private final AppUserRepository appUserRepository;
  private final EventProducer<TranscriptPdfRequested> eventProducer;

  public TranscriptResponse getTranscript(UUID studentId, int academicYear) {
    JStudent student =
        studentRepository
            .findById(studentId)
            .orElseThrow(() -> new NoSuchElementException("Student not found"));

    Set<UUID> groupIds = groupIdsForYear(studentId, academicYear);

    List<TranscriptCourseLine> lines =
        courseAssignmentRepository.findByAcademicYear(academicYear).stream()
            .filter(a -> groupIds.contains(a.getGroup().getId()))
            .map(a -> computeCourseLine(studentId, a))
            .toList();

    boolean complete = !lines.isEmpty() && lines.stream().allMatch(TranscriptCourseLine::complete);
    int totalCredits = lines.stream().mapToInt(TranscriptCourseLine::credits).sum();

    return new TranscriptResponse(
        studentId,
        student.getStd(),
        academicYear,
        lines,
        weightedAverage(lines),
        totalCredits,
        complete);
  }

  public FullTranscriptResponse getFullTranscript(UUID studentId) {
    JStudent student =
        studentRepository
            .findById(studentId)
            .orElseThrow(() -> new NoSuchElementException("Student not found"));

    Set<Integer> years =
        groupMembershipRepository.findByStudentIdOrderByStartDateAsc(studentId).stream()
            .flatMap(m -> yearsCoveredBy(m).stream())
            .collect(Collectors.toCollection(TreeSet::new));

    List<TranscriptResponse> yearTranscripts =
        years.stream().map(y -> getTranscript(studentId, y)).toList();

    boolean complete =
        !yearTranscripts.isEmpty()
            && yearTranscripts.stream().allMatch(TranscriptResponse::complete);
    int cumulativeCredits =
        yearTranscripts.stream().mapToInt(TranscriptResponse::totalCredits).sum();

    return new FullTranscriptResponse(
        studentId,
        student.getStd(),
        yearTranscripts,
        weightedAverageAcrossYears(yearTranscripts),
        cumulativeCredits,
        complete);
  }

  private Set<UUID> groupIdsForYear(UUID studentId, int academicYear) {
    LocalDate yearStart = LocalDate.of(academicYear, 1, 1);
    LocalDate yearEnd = LocalDate.of(academicYear, 12, 31);
    return groupMembershipRepository.findByStudentIdOrderByStartDateAsc(studentId).stream()
        .filter(
            m ->
                !m.getStartDate().isAfter(yearEnd)
                    && (m.getEndDate() == null || !m.getEndDate().isBefore(yearStart)))
        .map(m -> m.getGroup().getId())
        .collect(Collectors.toSet());
  }

  public void requestTranscriptPdf(UUID studentId, Integer academicYear) {
    var user =
        appUserRepository
            .findById(studentId)
            .orElseThrow(() -> new NoSuchElementException("Student not found"));

    var event =
        TranscriptPdfRequested.builder()
            .studentId(studentId.toString())
            .recipientEmail(user.getEmail())
            .academicYear(academicYear)
            .build();

    eventProducer.accept(List.of(event));
  }

  private List<Integer> yearsCoveredBy(JGroupMembership m) {
    int startYear = m.getStartDate().getYear();
    int endYear = m.getEndDate() != null ? m.getEndDate().getYear() : startYear;
    return IntStream.rangeClosed(startYear, endYear).boxed().toList();
  }

  private TranscriptCourseLine computeCourseLine(UUID studentId, JCourseAssignment assignment) {
    List<JExam> exams = examRepository.findByAssignmentId(assignment.getId());
    BigDecimal weightedSum = BigDecimal.ZERO;
    BigDecimal coveredCoefficient = BigDecimal.ZERO;
    boolean complete = !exams.isEmpty();

    for (JExam exam : exams) {
      var grade = gradeRepository.findByStudentIdAndExamIdAndCurrentTrue(studentId, exam.getId());
      if (grade.isPresent()) {
        weightedSum = weightedSum.add(grade.get().getValue().multiply(exam.getCoefficient()));
        coveredCoefficient = coveredCoefficient.add(exam.getCoefficient());
      } else {
        complete = false;
      }
    }

    Double average =
        coveredCoefficient.compareTo(BigDecimal.ZERO) > 0
            ? weightedSum.divide(coveredCoefficient, 2, RoundingMode.HALF_UP).doubleValue()
            : null;

    return new TranscriptCourseLine(
        assignment.getCourse().getRef(),
        assignment.getCourse().getTitle(),
        average,
        assignment.getCourse().getCredits(),
        complete);
  }

  private Double weightedAverage(List<TranscriptCourseLine> lines) {
    var graded = lines.stream().filter(l -> l.average() != null).toList();
    if (graded.isEmpty()) return null;
    double totalCredits = graded.stream().mapToInt(TranscriptCourseLine::credits).sum();
    double weightedSum = graded.stream().mapToDouble(l -> l.average() * l.credits()).sum();
    return totalCredits > 0 ? weightedSum / totalCredits : null;
  }

  private Double weightedAverageAcrossYears(List<TranscriptResponse> years) {
    var graded = years.stream().filter(y -> y.generalAverage() != null).toList();
    if (graded.isEmpty()) return null;
    double totalCredits = graded.stream().mapToInt(TranscriptResponse::totalCredits).sum();
    double weightedSum =
        graded.stream().mapToDouble(y -> y.generalAverage() * y.totalCredits()).sum();
    return totalCredits > 0 ? weightedSum / totalCredits : null;
  }
}
