package mg.school.hei.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.CourseRequest;
import mg.school.hei.endpoint.rest.controller.dto.CourseResponse;
import mg.school.hei.mapper.CourseMapper;
import mg.school.hei.model.Course;
import mg.school.hei.repository.CourseAssignmentRepository;
import mg.school.hei.repository.CourseRepository;
import mg.school.hei.repository.model.JCourse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {
  private final CourseRepository courseRepository;
  private final CourseAssignmentRepository courseAssignmentRepository;
  private final CourseMapper courseMapper;

  @Transactional
  public CourseResponse create(CourseRequest request) {
    JCourse saved =
        courseRepository.save(
            JCourse.builder()
                .ref(request.ref())
                .title(request.title())
                .credits(request.credits())
                .build());
    return toResponse(courseMapper.toModel(saved));
  }

  public List<CourseResponse> list() {
    return courseRepository.findAll().stream()
        .map(courseMapper::toModel)
        .map(this::toResponse)
        .toList();
  }

  public CourseResponse get(UUID id) {
    return toResponse(courseMapper.toModel(findOrThrow(id)));
  }

  @Transactional
  public CourseResponse update(UUID id, CourseRequest request) {
    JCourse entity = findOrThrow(id);
    entity.setRef(request.ref());
    entity.setTitle(request.title());
    entity.setCredits(request.credits());
    return toResponse(courseMapper.toModel(courseRepository.save(entity)));
  }

  @Transactional
  public void delete(UUID id) {
    findOrThrow(id);
    boolean hasAssignments =
        courseAssignmentRepository.findAll().stream()
            .anyMatch(a -> a.getCourse().getId().equals(id));
    if (hasAssignments) {
      throw new IllegalStateException("Course still has assignments attached");
    }
    courseRepository.deleteById(id);
  }

  private JCourse findOrThrow(UUID id) {
    return courseRepository
        .findById(id)
        .orElseThrow(() -> new NoSuchElementException("Course not found"));
  }

  private CourseResponse toResponse(Course c) {
    return new CourseResponse(c.id(), c.ref(), c.title(), c.credits());
  }
}
