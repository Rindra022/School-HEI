package mg.school.hei.endpoint.rest.controller.dto;

import java.util.List;
import java.util.UUID;

public record TranscriptResponse(
    UUID studentId,
    String std,
    Integer academicYear,
    List<TranscriptCourseLine> courses,
    Double generalAverage,
    Integer totalCredits,
    boolean complete) {}
