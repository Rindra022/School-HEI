package mg.school.hei.model;

import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record Course(UUID id, String ref, String title, Integer credits) {}
