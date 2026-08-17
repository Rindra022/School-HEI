package mg.school.hei.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.GroupMembershipRequest;
import mg.school.hei.endpoint.rest.controller.dto.GroupMembershipResponse;
import mg.school.hei.service.GroupMembershipService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class GroupMembershipController {
  private final GroupMembershipService groupMembershipService;

  @GetMapping("/group-memberships")
  public List<GroupMembershipResponse> list(@RequestParam UUID studentId) {
    return groupMembershipService.listByStudent(studentId);
  }

  @PostMapping("/group-memberships")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<GroupMembershipResponse> create(
      @Valid @RequestBody GroupMembershipRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(groupMembershipService.create(request));
  }

  @GetMapping("/group-memberships/{id}")
  public GroupMembershipResponse get(@PathVariable UUID id) {
    return groupMembershipService.get(id);
  }
}
