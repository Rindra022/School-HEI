package mg.school.hei.endpoint.rest.controller.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExamResponse(UUID id, UUID assignmentId, Instant dateExam, BigDecimal coefficient) {}
