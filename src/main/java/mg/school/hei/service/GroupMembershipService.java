package mg.school.hei.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.GroupMembershipRequest;
import mg.school.hei.endpoint.rest.controller.dto.GroupMembershipResponse;
import mg.school.hei.mapper.GroupMembershipMapper;
import mg.school.hei.model.GroupMembership;
import mg.school.hei.repository.AppGroupRepository;
import mg.school.hei.repository.GroupMembershipRepository;
import mg.school.hei.repository.StudentRepository;
import mg.school.hei.repository.model.JGroupMembership;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupMembershipService {
  private final GroupMembershipRepository groupMembershipRepository;
  private final StudentRepository studentRepository;
  private final AppGroupRepository appGroupRepository;
  private final GroupMembershipMapper groupMembershipMapper;

  @Transactional
  public GroupMembershipResponse create(GroupMembershipRequest request) {
    var jStudent =
        studentRepository
            .findById(request.studentId())
            .orElseThrow(() -> new NoSuchElementException("Student not found"));

    var jGroup =
        appGroupRepository
            .findById(request.groupId())
            .orElseThrow(() -> new NoSuchElementException("Group not found"));

    groupMembershipRepository.findByStudentIdOrderByStartDateAsc(jStudent.getId()).stream()
        .filter(m -> m.getEndDate() == null)
        .findFirst()
        .ifPresent(
            current -> {
              if (!current.getStartDate().isAfter(request.startDate())) {
                current.setEndDate(request.startDate().minusDays(1));
                groupMembershipRepository.save(current);
              }
            });

    JGroupMembership saved =
        groupMembershipRepository.save(
            JGroupMembership.builder()
                .student(jStudent)
                .group(jGroup)
                .startDate(request.startDate())
                .endDate(null)
                .build());

    return toResponse(groupMembershipMapper.toModel(saved));
  }

  public List<GroupMembershipResponse> listByStudent(UUID studentId) {
    return groupMembershipRepository.findByStudentIdOrderByStartDateAsc(studentId).stream()
        .map(groupMembershipMapper::toModel)
        .map(this::toResponse)
        .toList();
  }

  public GroupMembershipResponse get(UUID id) {
    JGroupMembership entity =
        groupMembershipRepository
            .findById(id)
            .orElseThrow(() -> new NoSuchElementException("Membership not found"));
    return toResponse(groupMembershipMapper.toModel(entity));
  }

  private GroupMembershipResponse toResponse(GroupMembership m) {
    return new GroupMembershipResponse(
        m.id(), m.student().id(), m.group().id(), m.startDate(), m.endDate());
  }
}
