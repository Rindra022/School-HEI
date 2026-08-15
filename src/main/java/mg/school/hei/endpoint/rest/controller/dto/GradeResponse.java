package mg.school.hei.endpoint.rest.controller.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GradeResponse(
    UUID id,
    UUID studentId,
    UUID examId,
    BigDecimal value,
    Instant gradedAt,
    String reason,
    UUID previousGradeId,
    boolean current) {}
