package mg.school.hei.repository.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "promotion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class JPromotion {
  @Id @GeneratedValue private UUID id;

  @Column(nullable = false, unique = true)
  private Integer year;
}
