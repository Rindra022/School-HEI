package mg.school.hei.endpoint.rest.controller.dto;

import jakarta.validation.constraints.NotNull;

public record PromotionRequest(@NotNull Integer year) {}