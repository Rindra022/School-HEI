package mg.school.hei.endpoint.rest.controller.dto;

import java.util.UUID;
import mg.school.hei.model.Track;

public record GroupResponse(UUID id, String ref, Track track) {}