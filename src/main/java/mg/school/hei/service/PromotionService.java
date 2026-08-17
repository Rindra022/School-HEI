package mg.school.hei.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mg.school.hei.endpoint.rest.controller.dto.PromotionRequest;
import mg.school.hei.endpoint.rest.controller.dto.PromotionResponse;
import mg.school.hei.mapper.PromotionMapper;
import mg.school.hei.model.Promotion;
import mg.school.hei.repository.PromotionRepository;
import mg.school.hei.repository.StudentRepository;
import mg.school.hei.repository.model.JPromotion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromotionService {
  private final PromotionRepository promotionRepository;
  private final StudentRepository studentRepository;
  private final PromotionMapper promotionMapper;

  @Transactional
  public PromotionResponse create(PromotionRequest request) {
    rejectIfYearTaken(request.year(), null);
    JPromotion saved = promotionRepository.save(JPromotion.builder().year(request.year()).build());
    return toResponse(promotionMapper.toModel(saved));
  }

  public List<PromotionResponse> list() {
    return promotionRepository.findAll().stream()
        .map(promotionMapper::toModel)
        .map(this::toResponse)
        .toList();
  }

  public PromotionResponse get(UUID id) {
    return toResponse(promotionMapper.toModel(findOrThrow(id)));
  }

  @Transactional
  public PromotionResponse update(UUID id, PromotionRequest request) {
    JPromotion entity = findOrThrow(id);
    rejectIfYearTaken(request.year(), id);
    entity.setYear(request.year());
    return toResponse(promotionMapper.toModel(promotionRepository.save(entity)));
  }

  @Transactional
  public void delete(UUID id) {
    findOrThrow(id);
    if (!studentRepository.findByPromotionId(id).isEmpty()) {
      throw new IllegalStateException("Promotion still has students attached");
    }
    promotionRepository.deleteById(id);
  }

  private void rejectIfYearTaken(Integer year, UUID excludingId) {
    promotionRepository
        .findByYear(year)
        .filter(p -> excludingId == null || !p.getId().equals(excludingId))
        .ifPresent(
            p -> {
              throw new IllegalArgumentException("Promotion for year " + year + " already exists");
            });
  }

  private JPromotion findOrThrow(UUID id) {
    return promotionRepository
        .findById(id)
        .orElseThrow(() -> new NoSuchElementException("Promotion not found"));
  }

  private PromotionResponse toResponse(Promotion p) {
    return new PromotionResponse(p.id(), p.year());
  }
}
