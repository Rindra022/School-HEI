package mg.school.hei.endpoint.rest.controller.dto;

import java.util.List;
import java.util.UUID;

public record FullTranscriptResponse(
    UUID studentId,
    String std,
    List<TranscriptResponse> years,
    Double cumulativeGeneralAverage,
    Integer cumulativeCredits,
    boolean complete) {}
