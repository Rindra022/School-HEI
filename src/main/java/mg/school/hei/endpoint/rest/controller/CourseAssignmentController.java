package mg.school.hei.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.CourseAssignmentRequest;
import mg.school.hei.endpoint.rest.controller.dto.CourseAssignmentResponse;
import mg.school.hei.service.CourseAssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CourseAssignmentController {
    private final CourseAssignmentService courseAssignmentService;

    @GetMapping("/course-assignments")
    public List<CourseAssignmentResponse> list(@RequestParam(required = false) Integer academicYear) {
        return courseAssignmentService.list(academicYear);
    }

    @PostMapping("/course-assignments")
    public ResponseEntity<CourseAssignmentResponse> create(
            @Valid @RequestBody CourseAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseAssignmentService.create(request));
    }

    @GetMapping("/course-assignments/{id}")
    public CourseAssignmentResponse get(@PathVariable UUID id) {
        return courseAssignmentService.get(id);
    }

    @DeleteMapping("/course-assignments/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        courseAssignmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}