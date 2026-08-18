package mg.school.hei.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.GroupRequest;
import mg.school.hei.endpoint.rest.controller.dto.GroupResponse;
import mg.school.hei.service.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class GroupController {
  private final GroupService groupService;

  @GetMapping("/groups")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public List<GroupResponse> list() {
    return groupService.list();
  }

  @PostMapping("/groups")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<GroupResponse> create(@Valid @RequestBody GroupRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(groupService.create(request));
  }

  @GetMapping("/groups/{id}")
  public GroupResponse get(@PathVariable UUID id) {
    return groupService.get(id);
  }

  @PatchMapping("/groups/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public GroupResponse update(@PathVariable UUID id, @Valid @RequestBody GroupRequest request) {
    return groupService.update(id, request);
  }

  @DeleteMapping("/groups/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    groupService.delete(id);
    return ResponseEntity.noContent().build();
  }
}