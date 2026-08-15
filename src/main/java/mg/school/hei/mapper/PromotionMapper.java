package mg.school.hei.mapper;

import mg.school.hei.model.Promotion;
import mg.school.hei.repository.model.JPromotion;
import org.springframework.stereotype.Component;

@Component
public class PromotionMapper {
  public Promotion toModel(JPromotion entity) {
    return Promotion.builder().id(entity.getId()).year(entity.getYear()).build();
  }

  public JPromotion toEntity(Promotion model) {
    return JPromotion.builder().id(model.id()).year(model.year()).build();
  }
}
