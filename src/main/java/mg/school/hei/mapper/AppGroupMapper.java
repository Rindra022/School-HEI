package mg.school.hei.mapper;

import mg.school.hei.model.AppGroup;
import mg.school.hei.repository.model.JAppGroup;
import org.springframework.stereotype.Component;

@Component
public class AppGroupMapper {
  public AppGroup toModel(JAppGroup entity) {
    return AppGroup.builder()
        .id(entity.getId())
        .ref(entity.getRef())
        .track(entity.getTrack())
        .build();
  }

  public JAppGroup toEntity(AppGroup model) {
    return JAppGroup.builder().id(model.id()).ref(model.ref()).track(model.track()).build();
  }
}
