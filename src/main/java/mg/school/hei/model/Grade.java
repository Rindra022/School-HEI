package mg.school.hei.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record Grade(
    UUID id,
    Student student,
    Exam exam,
    BigDecimal value,
    Instant gradedAt,
    String reason,
    Grade previousGrade,
    boolean current) {}
