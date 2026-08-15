package mg.school.hei.mapper;

import lombok.RequiredArgsConstructor;
import mg.school.hei.model.Grade;
import mg.school.hei.repository.model.JGrade;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GradeMapper {
  private final StudentMapper studentMapper;
  private final ExamMapper examMapper;

  public Grade toModel(JGrade entity) {
    if (entity == null) return null;
    return Grade.builder()
        .id(entity.getId())
        .student(studentMapper.toModel(entity.getStudent()))
        .exam(examMapper.toModel(entity.getExam()))
        .value(entity.getValue())
        .gradedAt(entity.getGradedAt())
        .reason(entity.getReason())
        .previousGrade(toModel(entity.getPreviousGrade()))
        .current(entity.isCurrent())
        .build();
  }

  public JGrade toEntity(Grade model) {
    if (model == null) return null;
    return JGrade.builder()
        .id(model.id())
        .student(studentMapper.toEntity(model.student()))
        .exam(examMapper.toEntity(model.exam()))
        .value(model.value())
        .gradedAt(model.gradedAt())
        .reason(model.reason())
        .previousGrade(toEntity(model.previousGrade()))
        .current(model.current())
        .build();
  }
}
