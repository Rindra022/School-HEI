package mg.school.hei.endpoint.rest.controller.health;

import static mg.school.hei.endpoint.rest.controller.health.PingController.KO;
import static mg.school.hei.endpoint.rest.controller.health.PingController.OK;

import lombok.AllArgsConstructor;
import mg.school.hei.PojaGenerated;
import mg.school.hei.repository.DummyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@PojaGenerated
@RestController
@AllArgsConstructor
public class HealthDbController {

  DummyRepository dummyRepository;

  @GetMapping("/health/db")
  public ResponseEntity<String> dummyTable_should_not_be_empty() {
    return dummyRepository.findAll().isEmpty() ? KO : OK;
  }
}
