package mg.school.hei.repository;

import java.util.List;
import java.util.UUID;
import mg.school.hei.repository.model.JCourseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseAssignmentRepository extends JpaRepository<JCourseAssignment, UUID> {
  List<JCourseAssignment> findByTeacherId(UUID teacherId);

  List<JCourseAssignment> findByAcademicYear(Integer academicYear);
}
