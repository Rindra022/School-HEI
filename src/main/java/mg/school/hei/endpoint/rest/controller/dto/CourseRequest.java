package mg.school.hei.endpoint.rest.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CourseRequest(
        @NotBlank String ref, @NotBlank String title, @NotNull @Min(1) Integer credits) {}