package mg.school.hei.endpoint.rest.controller.dto;

import java.util.UUID;

public record CourseResponse(UUID id, String ref, String title, Integer credits) {}