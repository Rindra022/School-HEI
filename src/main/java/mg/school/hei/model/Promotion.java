package mg.school.hei.model;

import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record Promotion(UUID id, Integer year) {}
