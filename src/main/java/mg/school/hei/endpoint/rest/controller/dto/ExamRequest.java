package mg.school.hei.endpoint.rest.controller.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record ExamRequest(
    @NotNull UUID assignmentId,
    @NotNull Instant dateExam,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") java.math.BigDecimal coefficient) {}
