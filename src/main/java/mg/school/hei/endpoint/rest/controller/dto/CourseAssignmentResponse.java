package mg.school.hei.endpoint.rest.controller.dto;

import java.util.UUID;

public record CourseAssignmentResponse(
        UUID id, UUID courseId, UUID teacherId, UUID groupId, Integer academicYear) {}