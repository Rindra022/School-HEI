package mg.school.hei.repository;

import java.util.List;
import java.util.UUID;
import mg.school.hei.repository.model.JExam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<JExam, UUID> {
  List<JExam> findByAssignmentId(UUID assignmentId);
}
