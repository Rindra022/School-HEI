package mg.school.hei.endpoint.rest.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.GraduateResponse;
import mg.school.hei.model.Track;
import mg.school.hei.service.GraduateExportService;
import mg.school.hei.service.GraduateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class GraduateController {
  private final GraduateService graduateService;
  private final GraduateExportService graduateExportService;

  @GetMapping("/promotions/{id}/graduates")
  @PreAuthorize("hasRole('ADMIN')")
  public List<GraduateResponse> listGraduates(
      @PathVariable UUID id, @RequestParam(required = false) Track track) {
    return graduateService.listGraduates(id, track);
  }

  @GetMapping("/promotions/{id}/graduates/export")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> exportGraduatesXlsx(@PathVariable UUID id) {
    var downloadUrl = graduateExportService.exportGraduatesXlsx(id);
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.LOCATION, downloadUrl.toString())
        .build();
  }
}
