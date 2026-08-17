package mg.school.hei.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.GradeRequest;
import mg.school.hei.endpoint.rest.controller.dto.GradeResponse;
import mg.school.hei.service.GradeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class GradeController {
  private final GradeService gradeService;

  @GetMapping("/grades")
  public List<GradeResponse> list(
      @RequestParam(required = false) UUID studentId, @RequestParam(required = false) UUID examId) {
    return gradeService.list(studentId, examId);
  }

  @PostMapping("/grades")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<GradeResponse> record(@Valid @RequestBody GradeRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(gradeService.record(request));
  }

  @GetMapping("/grades/{id}")
  public GradeResponse get(@PathVariable UUID id) {
    return gradeService.get(id);
  }

  @GetMapping("/grades/{id}/history")
  public List<GradeResponse> history(@PathVariable UUID id) {
    return gradeService.history(id);
  }
}
