package mg.school.hei.model;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record GroupMembership(
    UUID id, Student student, AppGroup group, LocalDate startDate, LocalDate endDate) {}
