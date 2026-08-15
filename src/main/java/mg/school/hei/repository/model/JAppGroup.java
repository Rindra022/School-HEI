package mg.school.hei.repository.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import mg.school.hei.model.Track;

@Entity
@Table(name = "app_group")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class JAppGroup {
  @Id @GeneratedValue private UUID id;

  @Column(nullable = false)
  private String ref;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Track track;
}
