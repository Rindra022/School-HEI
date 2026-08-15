package mg.school.hei.security.model;

import java.util.UUID;
import lombok.Builder;
import mg.school.hei.model.UserRole;

@Builder
public record Principal(UUID userId, UserRole role) {}
