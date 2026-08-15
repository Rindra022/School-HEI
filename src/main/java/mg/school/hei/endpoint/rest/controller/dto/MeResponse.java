package mg.school.hei.endpoint.rest.controller.dto;

import java.util.UUID;
import mg.school.hei.model.UserRole;

public record MeResponse(
    UUID id, String firstName, String lastName, String email, UserRole role, String std) {}
