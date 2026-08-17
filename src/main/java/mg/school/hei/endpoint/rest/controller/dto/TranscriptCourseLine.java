package mg.school.hei.endpoint.rest.controller.dto;

public record TranscriptCourseLine(
    String courseRef, String courseTitle, Double average, Integer credits, boolean complete) {}
