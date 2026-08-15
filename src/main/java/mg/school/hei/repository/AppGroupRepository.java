package mg.school.hei.repository;

import java.util.UUID;
import mg.school.hei.repository.model.JAppGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppGroupRepository extends JpaRepository<JAppGroup, UUID> {}
