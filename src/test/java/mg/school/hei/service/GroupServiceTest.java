package mg.school.hei.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mg.school.hei.endpoint.rest.controller.dto.GroupRequest;
import mg.school.hei.mapper.AppGroupMapper;
import mg.school.hei.model.Track;
import mg.school.hei.repository.AppGroupRepository;
import mg.school.hei.repository.CourseAssignmentRepository;
import mg.school.hei.repository.GroupMembershipRepository;
import mg.school.hei.repository.model.JAppGroup;
import mg.school.hei.repository.model.JGroupMembership;
import org.junit.jupiter.api.Test;

class GroupServiceTest {

  private final AppGroupRepository appGroupRepository = mock(AppGroupRepository.class);
  private final GroupMembershipRepository groupMembershipRepository =
      mock(GroupMembershipRepository.class);
  private final CourseAssignmentRepository courseAssignmentRepository =
      mock(CourseAssignmentRepository.class);
  private final AppGroupMapper appGroupMapper = new AppGroupMapper();

  private final GroupService service =
      new GroupService(
          appGroupRepository,
          groupMembershipRepository,
          courseAssignmentRepository,
          appGroupMapper);

  @Test
  void create_should_save_and_return_the_group() {
    when(appGroupRepository.save(any()))
        .thenAnswer(
            inv -> inv.getArgument(0, JAppGroup.class).toBuilder().id(UUID.randomUUID()).build());

    var response = service.create(new GroupRequest("K1", Track.EL));

    assertThat(response.ref()).isEqualTo("K1");
    assertThat(response.track()).isEqualTo(Track.EL);
  }

  @Test
  void delete_should_reject_when_group_has_memberships() {
    var id = UUID.randomUUID();
    when(appGroupRepository.findById(id))
        .thenReturn(Optional.of(JAppGroup.builder().id(id).ref("K1").track(Track.EL).build()));
    var membership = JGroupMembership.builder().group(JAppGroup.builder().id(id).build()).build();
    when(groupMembershipRepository.findAll()).thenReturn(List.of(membership));
    when(courseAssignmentRepository.findAll()).thenReturn(List.of());

    assertThatThrownBy(() -> service.delete(id)).isInstanceOf(IllegalStateException.class);
    verify(appGroupRepository, never()).deleteById(any());
  }

  @Test
  void delete_should_succeed_when_group_is_unreferenced() {
    var id = UUID.randomUUID();
    when(appGroupRepository.findById(id))
        .thenReturn(Optional.of(JAppGroup.builder().id(id).ref("K1").track(Track.EL).build()));
    when(groupMembershipRepository.findAll()).thenReturn(List.of());
    when(courseAssignmentRepository.findAll()).thenReturn(List.of());

    service.delete(id);

    verify(appGroupRepository).deleteById(id);
  }

  @Test
  void get_should_throw_when_group_is_unknown() {
    var id = UUID.randomUUID();
    when(appGroupRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(id)).isInstanceOf(java.util.NoSuchElementException.class);
  }
}
