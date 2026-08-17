package mg.school.hei.endpoint.rest.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.GraduateResponse;
import mg.school.hei.model.Track;
import mg.school.hei.service.GraduateService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class GraduateController {
  private final GraduateService graduateService;

  @GetMapping("/promotions/{id}/graduates")
  public List<GraduateResponse> listGraduates(
      @PathVariable UUID id, @RequestParam(required = false) Track track) {
    return graduateService.listGraduates(id, track);
  }
}
