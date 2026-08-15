package mg.school.hei.endpoint.rest.controller.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record GradeRequest(
    @NotNull UUID studentId,
    @NotNull UUID examId,
    @NotNull @DecimalMin("0.0") @DecimalMax("20.0") BigDecimal value,
    String reason) {}
