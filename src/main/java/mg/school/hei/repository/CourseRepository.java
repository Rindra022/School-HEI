package mg.school.hei.repository;

import java.util.UUID;
import mg.school.hei.repository.model.JCourse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<JCourse, UUID> {}
