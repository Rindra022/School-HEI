package mg.school.hei.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mg.school.hei.repository.model.JGrade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeRepository extends JpaRepository<JGrade, UUID> {
  Optional<JGrade> findByStudentIdAndExamIdAndCurrentTrue(UUID studentId, UUID examId);

  List<JGrade> findByStudentIdAndCurrentTrue(UUID studentId);
}
