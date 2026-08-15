package mg.school.hei.mapper;

import lombok.RequiredArgsConstructor;
import mg.school.hei.model.GroupMembership;
import mg.school.hei.repository.model.JGroupMembership;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GroupMembershipMapper {
  private final StudentMapper studentMapper;
  private final AppGroupMapper appGroupMapper;

  public GroupMembership toModel(JGroupMembership entity) {
    return GroupMembership.builder()
        .id(entity.getId())
        .student(studentMapper.toModel(entity.getStudent()))
        .group(appGroupMapper.toModel(entity.getGroup()))
        .startDate(entity.getStartDate())
        .endDate(entity.getEndDate())
        .build();
  }

  public JGroupMembership toEntity(GroupMembership model) {
    return JGroupMembership.builder()
        .id(model.id())
        .student(studentMapper.toEntity(model.student()))
        .group(appGroupMapper.toEntity(model.group()))
        .startDate(model.startDate())
        .endDate(model.endDate())
        .build();
  }
}
