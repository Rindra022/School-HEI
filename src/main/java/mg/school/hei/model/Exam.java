package mg.school.hei.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record Exam(
    UUID id, CourseAssignment assignment, Instant dateExam, BigDecimal coefficient) {}
