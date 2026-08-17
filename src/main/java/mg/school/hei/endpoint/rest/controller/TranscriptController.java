package mg.school.hei.endpoint.rest.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.FullTranscriptResponse;
import mg.school.hei.endpoint.rest.controller.dto.TranscriptResponse;
import mg.school.hei.service.TranscriptService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TranscriptController {
  private final TranscriptService transcriptService;

  @GetMapping("/students/{id}/transcript")
  public TranscriptResponse getTranscript(@PathVariable UUID id, @RequestParam int academicYear) {
    return transcriptService.getTranscript(id, academicYear);
  }

  @GetMapping("/students/{id}/transcript/full")
  public FullTranscriptResponse getFullTranscript(@PathVariable UUID id) {
    return transcriptService.getFullTranscript(id);
  }
}
