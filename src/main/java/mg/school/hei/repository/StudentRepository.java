package mg.school.hei.repository;

import java.util.UUID;
import mg.school.hei.repository.model.JStudent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<JStudent, UUID> {}
