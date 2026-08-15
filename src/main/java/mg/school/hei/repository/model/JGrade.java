package mg.school.hei.repository.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "grade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class JGrade {
  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", nullable = false)
  private JStudent student;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exam_id", nullable = false)
  private JExam exam;

  @Column(nullable = false)
  private BigDecimal value;

  @Column(name = "graded_at", nullable = false)
  private Instant gradedAt;

  private String reason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "previous_grade_id")
  private JGrade previousGrade;

  @Column(name = "is_current", nullable = false)
  private boolean current;
}
