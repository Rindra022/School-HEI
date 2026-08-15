package mg.school.hei.mapper;

import lombok.RequiredArgsConstructor;
import mg.school.hei.model.CourseAssignment;
import mg.school.hei.repository.model.JCourseAssignment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseAssignmentMapper {
  private final CourseMapper courseMapper;
  private final AppUserMapper appUserMapper;
  private final AppGroupMapper appGroupMapper;

  public CourseAssignment toModel(JCourseAssignment entity) {
    return CourseAssignment.builder()
        .id(entity.getId())
        .course(courseMapper.toModel(entity.getCourse()))
        .teacher(appUserMapper.toModel(entity.getTeacher()))
        .group(appGroupMapper.toModel(entity.getGroup()))
        .academicYear(entity.getAcademicYear())
        .build();
  }

  public JCourseAssignment toEntity(CourseAssignment model) {
    return JCourseAssignment.builder()
        .id(model.id())
        .course(courseMapper.toEntity(model.course()))
        .teacher(appUserMapper.toEntity(model.teacher()))
        .group(appGroupMapper.toEntity(model.group()))
        .academicYear(model.academicYear())
        .build();
  }
}
