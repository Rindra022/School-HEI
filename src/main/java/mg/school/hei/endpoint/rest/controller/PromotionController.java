package mg.school.hei.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.PromotionRequest;
import mg.school.hei.endpoint.rest.controller.dto.PromotionResponse;
import mg.school.hei.service.PromotionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PromotionController {
  private final PromotionService promotionService;

  @GetMapping("/promotions")
  public List<PromotionResponse> list() {
    return promotionService.list();
  }

  @PostMapping("/promotions")
  public ResponseEntity<PromotionResponse> create(@Valid @RequestBody PromotionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.create(request));
  }

  @GetMapping("/promotions/{id}")
  public PromotionResponse get(@PathVariable UUID id) {
    return promotionService.get(id);
  }

  @PatchMapping("/promotions/{id}")
  public PromotionResponse update(
      @PathVariable UUID id, @Valid @RequestBody PromotionRequest request) {
    return promotionService.update(id, request);
  }

  @DeleteMapping("/promotions/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    promotionService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
