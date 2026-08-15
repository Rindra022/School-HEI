package mg.school.hei.repository;

import java.util.Optional;
import java.util.UUID;
import mg.school.hei.repository.model.JPromotion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRepository extends JpaRepository<JPromotion, UUID> {
  Optional<JPromotion> findByYear(Integer year);
}
