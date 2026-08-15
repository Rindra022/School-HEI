package mg.school.hei.mapper;

import lombok.RequiredArgsConstructor;
import mg.school.hei.model.Exam;
import mg.school.hei.repository.model.JExam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExamMapper {
  private final CourseAssignmentMapper courseAssignmentMapper;

  public Exam toModel(JExam entity) {
    return Exam.builder()
        .id(entity.getId())
        .assignment(courseAssignmentMapper.toModel(entity.getAssignment()))
        .dateExam(entity.getDateExam())
        .coefficient(entity.getCoefficient())
        .build();
  }

  public JExam toEntity(Exam model) {
    return JExam.builder()
        .id(model.id())
        .assignment(courseAssignmentMapper.toEntity(model.assignment()))
        .dateExam(model.dateExam())
        .coefficient(model.coefficient())
        .build();
  }
}
