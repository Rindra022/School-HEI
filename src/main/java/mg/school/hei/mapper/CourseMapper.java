package mg.school.hei.mapper;

import mg.school.hei.model.Course;
import mg.school.hei.repository.model.JCourse;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {
  public Course toModel(JCourse entity) {
    return Course.builder()
        .id(entity.getId())
        .ref(entity.getRef())
        .title(entity.getTitle())
        .credits(entity.getCredits())
        .build();
  }

  public JCourse toEntity(Course model) {
    return JCourse.builder()
        .id(model.id())
        .ref(model.ref())
        .title(model.title())
        .credits(model.credits())
        .build();
  }
}
