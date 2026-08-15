package mg.school.hei.repository;

import java.util.Optional;
import java.util.UUID;
import mg.school.hei.repository.model.JAppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<JAppUser, UUID> {
  Optional<JAppUser> findByEmail(String email);
}
