package mg.school.hei.repository;

import java.util.List;
import java.util.UUID;
import mg.school.hei.repository.model.JGroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMembershipRepository extends JpaRepository<JGroupMembership, UUID> {
  List<JGroupMembership> findByStudentIdOrderByStartDateAsc(UUID studentId);
}
