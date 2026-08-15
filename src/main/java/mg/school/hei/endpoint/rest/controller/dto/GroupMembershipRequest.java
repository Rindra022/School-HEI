package mg.school.hei.endpoint.rest.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record GroupMembershipRequest(
    @NotNull UUID studentId, @NotNull UUID groupId, @NotNull LocalDate startDate) {}
