package mg.school.hei.endpoint.rest.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record RegisterRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    LocalDate birthdate,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password,
    String phone,
    @NotNull UUID promotionId) {}
