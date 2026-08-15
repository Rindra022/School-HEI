package mg.school.hei.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.GroupMembershipRequest;
import mg.school.hei.endpoint.rest.controller.dto.GroupMembershipResponse;
import mg.school.hei.repository.AppGroupRepository;
import mg.school.hei.repository.GroupMembershipRepository;
import mg.school.hei.repository.StudentRepository;
import mg.school.hei.repository.model.JAppGroup;
import mg.school.hei.repository.model.JGroupMembership;
import mg.school.hei.repository.model.JStudent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupMembershipService {
  private final GroupMembershipRepository groupMembershipRepository;
  private final StudentRepository studentRepository;
  private final AppGroupRepository appGroupRepository;

  @Transactional
  public GroupMembershipResponse create(GroupMembershipRequest request) {
    JStudent student =
        studentRepository
            .findById(request.studentId())
            .orElseThrow(() -> new NoSuchElementException("Student not found"));

    JAppGroup group =
        appGroupRepository
            .findById(request.groupId())
            .orElseThrow(() -> new NoSuchElementException("Group not found"));

    // Close the current active membership (if any) the day before the new one starts
    groupMembershipRepository.findByStudentIdOrderByStartDateAsc(student.getId()).stream()
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
                .student(student)
                .group(group)
                .startDate(request.startDate())
                .endDate(null)
                .build());

    return toResponse(saved);
  }

  public List<GroupMembershipResponse> listByStudent(UUID studentId) {
    return groupMembershipRepository.findByStudentIdOrderByStartDateAsc(studentId).stream()
        .map(this::toResponse)
        .toList();
  }

  public GroupMembershipResponse get(UUID id) {
    return groupMembershipRepository
        .findById(id)
        .map(this::toResponse)
        .orElseThrow(() -> new NoSuchElementException("Membership not found"));
  }

  private GroupMembershipResponse toResponse(JGroupMembership m) {
    return new GroupMembershipResponse(
        m.getId(), m.getStudent().getId(), m.getGroup().getId(), m.getStartDate(), m.getEndDate());
  }
}
