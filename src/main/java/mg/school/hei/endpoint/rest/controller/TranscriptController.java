package mg.school.hei.endpoint.rest.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.FullTranscriptResponse;
import mg.school.hei.endpoint.rest.controller.dto.TranscriptResponse;
import mg.school.hei.service.TranscriptService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TranscriptController {
  private final TranscriptService transcriptService;

  @GetMapping("/students/{id}/transcript")
  @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.userId()")
  public TranscriptResponse getTranscript(@PathVariable UUID id, @RequestParam int academicYear) {
    return transcriptService.getTranscript(id, academicYear);
  }

  @GetMapping("/students/{id}/transcript/full")
  @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.userId()")
  public FullTranscriptResponse getFullTranscript(@PathVariable UUID id) {
    return transcriptService.getFullTranscript(id);
  }

  @PostMapping("/students/{id}/transcript/pdf")
  @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.userId()")
  public ResponseEntity<Void> requestTranscriptPdf(
      @PathVariable UUID id, @RequestParam(required = false) Integer academicYear) {
    transcriptService.requestTranscriptPdf(id, academicYear);
    return ResponseEntity.accepted().build();
  }
}
