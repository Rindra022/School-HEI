package mg.school.hei.endpoint.rest.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CourseAssignmentRequest(
        @NotNull UUID courseId, @NotNull UUID teacherId, @NotNull UUID groupId, @NotNull Integer academicYear) {}