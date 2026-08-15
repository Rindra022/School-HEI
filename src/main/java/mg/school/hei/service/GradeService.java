package mg.school.hei.service;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.GradeRequest;
import mg.school.hei.endpoint.rest.controller.dto.GradeResponse;
import mg.school.hei.mapper.GradeMapper;
import mg.school.hei.model.Grade;
import mg.school.hei.repository.ExamRepository;
import mg.school.hei.repository.GradeRepository;
import mg.school.hei.repository.StudentRepository;
import mg.school.hei.repository.model.JExam;
import mg.school.hei.repository.model.JGrade;
import mg.school.hei.repository.model.JStudent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GradeService {
  private final GradeRepository gradeRepository;
  private final StudentRepository studentRepository;
  private final ExamRepository examRepository;
  private final GradeMapper gradeMapper;

  @Transactional
  public GradeResponse record(GradeRequest request) {
    JStudent student =
        studentRepository
            .findById(request.studentId())
            .orElseThrow(() -> new NoSuchElementException("Student not found"));

    JExam exam =
        examRepository
            .findById(request.examId())
            .orElseThrow(() -> new NoSuchElementException("Exam not found"));

    Optional<JGrade> current =
        gradeRepository.findByStudentIdAndExamIdAndCurrentTrue(student.getId(), exam.getId());

    if (current.isPresent() && (request.reason() == null || request.reason().isBlank())) {
      throw new IllegalArgumentException("Reason is required when amending an existing grade");
    }

    current.ifPresent(
        previous -> {
          previous.setCurrent(false);
          gradeRepository.save(previous);
        });

    JGrade saved =
        gradeRepository.save(
            JGrade.builder()
                .student(student)
                .exam(exam)
                .value(request.value())
                .gradedAt(Instant.now())
                .reason(request.reason())
                .previousGrade(current.orElse(null))
                .current(true)
                .build());

    return toResponse(gradeMapper.toModel(saved));
  }

  public List<GradeResponse> list(UUID studentId) {
    return gradeRepository.findByStudentIdAndCurrentTrue(studentId).stream()
        .map(gradeMapper::toModel)
        .map(this::toResponse)
        .toList();
  }

  public GradeResponse get(UUID id) {
    JGrade entity =
        gradeRepository
            .findById(id)
            .orElseThrow(() -> new NoSuchElementException("Grade not found"));
    return toResponse(gradeMapper.toModel(entity));
  }

  public List<GradeResponse> history(UUID id) {
    JGrade entity =
        gradeRepository
            .findById(id)
            .orElseThrow(() -> new NoSuchElementException("Grade not found"));

    LinkedList<GradeResponse> chain = new LinkedList<>();
    JGrade cursor = entity;
    while (cursor != null) {
      chain.addFirst(toResponse(gradeMapper.toModel(cursor)));
      cursor = cursor.getPreviousGrade();
    }
    return chain;
  }

  private GradeResponse toResponse(Grade g) {
    return new GradeResponse(
        g.id(),
        g.student().id(),
        g.exam().id(),
        g.value(),
        g.gradedAt(),
        g.reason(),
        g.previousGrade() != null ? g.previousGrade().id() : null,
        g.current());
  }
}
