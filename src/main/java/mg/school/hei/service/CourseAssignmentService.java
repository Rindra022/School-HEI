package mg.school.hei.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.CourseAssignmentRequest;
import mg.school.hei.endpoint.rest.controller.dto.CourseAssignmentResponse;
import mg.school.hei.mapper.CourseAssignmentMapper;
import mg.school.hei.model.CourseAssignment;
import mg.school.hei.model.UserRole;
import mg.school.hei.repository.*;
import mg.school.hei.repository.model.JAppGroup;
import mg.school.hei.repository.model.JAppUser;
import mg.school.hei.repository.model.JCourse;
import mg.school.hei.repository.model.JCourseAssignment;
import mg.school.hei.security.model.Principal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseAssignmentService {
  private final CourseAssignmentRepository courseAssignmentRepository;
  private final CourseRepository courseRepository;
  private final AppUserRepository appUserRepository;
  private final AppGroupRepository appGroupRepository;
  private final ExamRepository examRepository;
  private final CourseAssignmentMapper courseAssignmentMapper;

  @Transactional
  public CourseAssignmentResponse create(CourseAssignmentRequest request) {
    JCourse course =
        courseRepository
            .findById(request.courseId())
            .orElseThrow(() -> new NoSuchElementException("Course not found"));
    JAppUser teacher =
        appUserRepository
            .findById(request.teacherId())
            .orElseThrow(() -> new NoSuchElementException("Teacher not found"));
    JAppGroup group =
        appGroupRepository
            .findById(request.groupId())
            .orElseThrow(() -> new NoSuchElementException("Group not found"));

    boolean alreadyAssigned =
        courseAssignmentRepository.findByAcademicYear(request.academicYear()).stream()
            .anyMatch(
                a ->
                    a.getCourse().getId().equals(course.getId())
                        && a.getTeacher().getId().equals(teacher.getId())
                        && a.getGroup().getId().equals(group.getId()));
    if (alreadyAssigned) {
      throw new IllegalArgumentException("This assignment already exists for this academic year");
    }

    JCourseAssignment saved =
        courseAssignmentRepository.save(
            JCourseAssignment.builder()
                .course(course)
                .teacher(teacher)
                .group(group)
                .academicYear(request.academicYear())
                .build());

    return toResponse(courseAssignmentMapper.toModel(saved));
  }

  public List<CourseAssignmentResponse> list(Integer academicYear) {
    var assignments =
        academicYear != null
            ? courseAssignmentRepository.findByAcademicYear(academicYear)
            : courseAssignmentRepository.findAll();

    Principal principal =
        (Principal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal.role() == UserRole.TEACHER) {
      assignments =
          assignments.stream()
              .filter(a -> a.getTeacher().getId().equals(principal.userId()))
              .toList();
    }

    return assignments.stream().map(courseAssignmentMapper::toModel).map(this::toResponse).toList();
  }

  public CourseAssignmentResponse get(UUID id) {
    return toResponse(courseAssignmentMapper.toModel(findOrThrow(id)));
  }

  @Transactional
  public void delete(UUID id) {
    findOrThrow(id);
    if (!examRepository.findByAssignmentId(id).isEmpty()) {
      throw new IllegalStateException("Assignment still has exams attached");
    }
    courseAssignmentRepository.deleteById(id);
  }

  private JCourseAssignment findOrThrow(UUID id) {
    return courseAssignmentRepository
        .findById(id)
        .orElseThrow(() -> new NoSuchElementException("Course assignment not found"));
  }

  private CourseAssignmentResponse toResponse(CourseAssignment a) {
    return new CourseAssignmentResponse(
        a.id(), a.course().id(), a.teacher().id(), a.group().id(), a.academicYear());
  }
}
