package mg.school.hei.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.ExamRequest;
import mg.school.hei.endpoint.rest.controller.dto.ExamResponse;
import mg.school.hei.repository.CourseAssignmentRepository;
import mg.school.hei.repository.ExamRepository;
import mg.school.hei.repository.model.JCourseAssignment;
import mg.school.hei.repository.model.JExam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExamService {
  private final ExamRepository examRepository;
  private final CourseAssignmentRepository courseAssignmentRepository;

  @Transactional
  public ExamResponse create(ExamRequest request) {
    JCourseAssignment assignment =
        courseAssignmentRepository
            .findById(request.assignmentId())
            .orElseThrow(() -> new NoSuchElementException("Course assignment not found"));

    BigDecimal existingSum =
        examRepository.findByAssignmentId(assignment.getId()).stream()
            .map(JExam::getCoefficient)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (existingSum.add(request.coefficient()).compareTo(BigDecimal.ONE) > 0) {
      throw new IllegalArgumentException("Sum of coefficients for this assignment would exceed 1");
    }

    JExam saved =
        examRepository.save(
            JExam.builder()
                .assignment(assignment)
                .dateExam(request.dateExam())
                .coefficient(request.coefficient())
                .build());

    return toResponse(saved);
  }

  public List<ExamResponse> listByAssignment(UUID assignmentId) {
    return examRepository.findByAssignmentId(assignmentId).stream().map(this::toResponse).toList();
  }

  public ExamResponse get(UUID id) {
    return examRepository
        .findById(id)
        .map(this::toResponse)
        .orElseThrow(() -> new NoSuchElementException("Exam not found"));
  }

  private ExamResponse toResponse(JExam e) {
    return new ExamResponse(
        e.getId(), e.getAssignment().getId(), e.getDateExam(), e.getCoefficient());
  }
}
