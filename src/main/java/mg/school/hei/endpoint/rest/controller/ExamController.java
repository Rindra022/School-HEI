package mg.school.hei.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.ExamRequest;
import mg.school.hei.endpoint.rest.controller.dto.ExamResponse;
import mg.school.hei.service.ExamService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ExamController {
  private final ExamService examService;

  @GetMapping("/exams")
  public List<ExamResponse> list(@RequestParam UUID assignmentId) {
    return examService.listByAssignment(assignmentId);
  }

  @PostMapping("/exams")
  public ResponseEntity<ExamResponse> create(@Valid @RequestBody ExamRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(examService.create(request));
  }

  @GetMapping("/exams/{id}")
  public ExamResponse get(@PathVariable UUID id) {
    return examService.get(id);
  }

  @DeleteMapping("/exams/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    examService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
