package mg.school.hei.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record AppUser(
    UUID id,
    String firstName,
    String lastName,
    LocalDate birthdate,
    String email,
    String password,
    String phone,
    UserRole role,
    Instant createdAt) {}
