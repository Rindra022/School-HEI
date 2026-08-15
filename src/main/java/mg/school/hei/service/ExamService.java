package mg.school.hei.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.ExamRequest;
import mg.school.hei.endpoint.rest.controller.dto.ExamResponse;
import mg.school.hei.mapper.ExamMapper;
import mg.school.hei.model.Exam;
import mg.school.hei.repository.CourseAssignmentRepository;
import mg.school.hei.repository.ExamRepository;
import mg.school.hei.repository.GradeRepository;
import mg.school.hei.repository.model.JCourseAssignment;
import mg.school.hei.repository.model.JExam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExamService {
  private final ExamRepository examRepository;
  private final CourseAssignmentRepository courseAssignmentRepository;
  private final GradeRepository gradeRepository;
  private final ExamMapper examMapper;

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

    return toResponse(examMapper.toModel(saved));
  }

  public List<ExamResponse> listByAssignment(UUID assignmentId) {
    return examRepository.findByAssignmentId(assignmentId).stream()
        .map(examMapper::toModel)
        .map(this::toResponse)
        .toList();
  }

  public ExamResponse get(UUID id) {
    JExam entity =
        examRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Exam not found"));
    return toResponse(examMapper.toModel(entity));
  }

  @Transactional
  public void delete(UUID id) {
    if (!examRepository.existsById(id)) {
      throw new NoSuchElementException("Exam not found");
    }

    boolean hasGrades = !gradeRepository.findByExamId(id).isEmpty();
    if (hasGrades) {
      throw new IllegalStateException("Exam already has grades recorded");
    }

    examRepository.deleteById(id);
  }

  private ExamResponse toResponse(Exam e) {
    return new ExamResponse(e.id(), e.assignment().id(), e.dateExam(), e.coefficient());
  }
}
