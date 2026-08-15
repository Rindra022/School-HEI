package mg.school.hei.endpoint.rest.controller.dto;

import java.time.LocalDate;
import java.util.UUID;

public record GroupMembershipResponse(
    UUID id, UUID studentId, UUID groupId, LocalDate startDate, LocalDate endDate) {}
