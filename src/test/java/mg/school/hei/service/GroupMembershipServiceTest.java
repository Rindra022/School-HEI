package mg.school.hei.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mg.school.hei.endpoint.rest.controller.dto.GroupMembershipRequest;
import mg.school.hei.mapper.GroupMembershipMapper;
import mg.school.hei.model.AppGroup;
import mg.school.hei.model.GroupMembership;
import mg.school.hei.model.Student;
import mg.school.hei.repository.AppGroupRepository;
import mg.school.hei.repository.GroupMembershipRepository;
import mg.school.hei.repository.StudentRepository;
import mg.school.hei.repository.model.JAppGroup;
import mg.school.hei.repository.model.JGroupMembership;
import mg.school.hei.repository.model.JStudent;
import org.junit.jupiter.api.Test;

class GroupMembershipServiceTest {

  private final GroupMembershipRepository groupMembershipRepository =
      mock(GroupMembershipRepository.class);
  private final StudentRepository studentRepository = mock(StudentRepository.class);
  private final AppGroupRepository appGroupRepository = mock(AppGroupRepository.class);
  private final GroupMembershipMapper groupMembershipMapper = mock(GroupMembershipMapper.class);

  private final GroupMembershipService service =
      new GroupMembershipService(
          groupMembershipRepository, studentRepository, appGroupRepository, groupMembershipMapper);

  @Test
  void create_should_close_the_active_membership_when_moving_forward_in_time() {
    var studentId = UUID.randomUUID();
    var groupId = UUID.randomUUID();
    var student = JStudent.builder().id(studentId).build();
    var group = JAppGroup.builder().id(groupId).build();
    var previousGroup = JAppGroup.builder().id(UUID.randomUUID()).build();
    var active =
        JGroupMembership.builder()
            .group(previousGroup)
            .startDate(LocalDate.of(2024, 9, 1))
            .endDate(null)
            .build();
    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(appGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
    when(groupMembershipRepository.findByStudentIdOrderByStartDateAsc(studentId))
        .thenReturn(List.of(active));
    when(groupMembershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(groupMembershipMapper.toModel(any()))
        .thenReturn(
            new GroupMembership(
                UUID.randomUUID(),
                new Student(studentId, "STD24001", null, null),
                new AppGroup(groupId, "K3", null),
                LocalDate.of(2024, 11, 15),
                null));

    var request = new GroupMembershipRequest(studentId, groupId, LocalDate.of(2024, 11, 15));

    service.create(request);

    assertThat(active.getEndDate()).isEqualTo(LocalDate.of(2024, 11, 14));
    verify(groupMembershipRepository, times(2)).save(any());
  }

  @Test
  void create_should_reject_backdating_before_the_currently_active_membership() {
    var studentId = UUID.randomUUID();
    var groupId = UUID.randomUUID();
    var previousGroup = JAppGroup.builder().id(UUID.randomUUID()).build();
    var active =
        JGroupMembership.builder()
            .group(previousGroup)
            .startDate(LocalDate.of(2024, 11, 1))
            .endDate(null)
            .build();
    when(studentRepository.findById(studentId))
        .thenReturn(Optional.of(JStudent.builder().id(studentId).build()));
    when(appGroupRepository.findById(groupId))
        .thenReturn(Optional.of(JAppGroup.builder().id(groupId).build()));
    when(groupMembershipRepository.findByStudentIdOrderByStartDateAsc(studentId))
        .thenReturn(List.of(active));

    var request = new GroupMembershipRequest(studentId, groupId, LocalDate.of(2024, 9, 1));

    assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalArgumentException.class);
    verify(groupMembershipRepository, never()).save(any());
  }

  @Test
  void create_should_reject_an_unknown_student() {
    var studentId = UUID.randomUUID();
    var groupId = UUID.randomUUID();
    when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

    var request = new GroupMembershipRequest(studentId, groupId, LocalDate.now());

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(java.util.NoSuchElementException.class);
  }
}
