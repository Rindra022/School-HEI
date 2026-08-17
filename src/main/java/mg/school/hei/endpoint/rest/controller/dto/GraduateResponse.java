package mg.school.hei.endpoint.rest.controller.dto;

import mg.school.hei.model.Track;

public record GraduateResponse(
    int rank, String std, String firstName, String lastName, double generalAverage, Track track) {}
