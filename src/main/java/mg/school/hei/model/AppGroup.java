package mg.school.hei.model;

import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record AppGroup(UUID id, String ref, Track track) {}
