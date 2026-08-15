package mg.school.hei.mapper;

import lombok.RequiredArgsConstructor;
import mg.school.hei.model.Student;
import mg.school.hei.repository.model.JStudent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentMapper {
  private final PromotionMapper promotionMapper;

  public Student toModel(JStudent entity) {
    return Student.builder()
        .id(entity.getId())
        .std(entity.getStd())
        .promotion(promotionMapper.toModel(entity.getPromotion()))
        .build();
  }

  public JStudent toEntity(Student model) {
    return JStudent.builder()
        .id(model.id())
        .std(model.std())
        .promotion(promotionMapper.toEntity(model.promotion()))
        .build();
  }
}
