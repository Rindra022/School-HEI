package mg.school.hei.endpoint.rest.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import mg.school.hei.model.Track;

public record GroupRequest(@NotBlank String ref, @NotNull Track track) {}
