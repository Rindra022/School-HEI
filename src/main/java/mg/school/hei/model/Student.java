package mg.school.hei.model;

import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record Student(UUID id, String std, Promotion promotion, AppUser appUser) {}
