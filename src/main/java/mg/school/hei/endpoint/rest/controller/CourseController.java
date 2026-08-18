package mg.school.hei.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.CourseRequest;
import mg.school.hei.endpoint.rest.controller.dto.CourseResponse;
import mg.school.hei.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CourseController {
  private final CourseService courseService;

  @GetMapping("/courses")
  public List<CourseResponse> list() {
    return courseService.list();
  }

  @PostMapping("/courses")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(request));
  }

  @GetMapping("/courses/{id}")
  public CourseResponse get(@PathVariable UUID id) {
    return courseService.get(id);
  }

  @PatchMapping("/courses/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public CourseResponse update(@PathVariable UUID id, @Valid @RequestBody CourseRequest request) {
    return courseService.update(id, request);
  }

  @DeleteMapping("/courses/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    courseService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
