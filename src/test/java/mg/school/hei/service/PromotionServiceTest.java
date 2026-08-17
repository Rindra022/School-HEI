package mg.school.hei.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mg.school.hei.endpoint.rest.controller.dto.PromotionRequest;
import mg.school.hei.endpoint.rest.controller.dto.PromotionResponse;
import mg.school.hei.mapper.PromotionMapper;
import mg.school.hei.repository.PromotionRepository;
import mg.school.hei.repository.StudentRepository;
import mg.school.hei.repository.model.JPromotion;
import mg.school.hei.repository.model.JStudent;
import org.junit.jupiter.api.Test;

class PromotionServiceTest {

  private final PromotionRepository promotionRepository = mock(PromotionRepository.class);
  private final StudentRepository studentRepository = mock(StudentRepository.class);
  private final PromotionMapper promotionMapper = new PromotionMapper();

  private final PromotionService service =
      new PromotionService(promotionRepository, studentRepository, promotionMapper);

  @Test
  void create_should_reject_a_duplicate_year() {
    when(promotionRepository.findByYear(2024))
        .thenReturn(Optional.of(JPromotion.builder().id(UUID.randomUUID()).year(2024).build()));

    assertThatThrownBy(() -> service.create(new PromotionRequest(2024)))
        .isInstanceOf(IllegalArgumentException.class);
    verify(promotionRepository, never()).save(any());
  }

  @Test
  void create_should_save_a_new_promotion() {
    when(promotionRepository.findByYear(2025)).thenReturn(Optional.empty());
    when(promotionRepository.save(any()))
        .thenAnswer(
            inv -> inv.getArgument(0, JPromotion.class).toBuilder().id(UUID.randomUUID()).build());

    var response = service.create(new PromotionRequest(2025));

    assertThat(response.year()).isEqualTo(2025);
  }

  @Test
  void update_should_allow_keeping_the_same_year() {
    var id = UUID.randomUUID();
    var entity = JPromotion.builder().id(id).year(2024).build();
    when(promotionRepository.findById(id)).thenReturn(Optional.of(entity));
    when(promotionRepository.findByYear(2024)).thenReturn(Optional.of(entity));
    when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var response = service.update(id, new PromotionRequest(2024));

    assertThat(response.year()).isEqualTo(2024);
  }

  @Test
  void delete_should_reject_when_promotion_has_students() {
    var id = UUID.randomUUID();
    when(promotionRepository.findById(id))
        .thenReturn(Optional.of(JPromotion.builder().id(id).year(2024).build()));
    when(studentRepository.findByPromotionId(id)).thenReturn(List.of(new JStudent()));

    assertThatThrownBy(() -> service.delete(id)).isInstanceOf(IllegalStateException.class);
    verify(promotionRepository, never()).deleteById(any());
  }

  @Test
  void delete_should_reject_an_unknown_promotion() {
    var id = UUID.randomUUID();
    when(promotionRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.delete(id))
        .isInstanceOf(java.util.NoSuchElementException.class);
  }

  @Test
  void delete_should_succeed_when_no_students() {
    var id = UUID.randomUUID();
    when(promotionRepository.findById(id))
        .thenReturn(Optional.of(JPromotion.builder().id(id).year(2024).build()));
    when(studentRepository.findByPromotionId(id)).thenReturn(List.of());

    assertDoesNotThrow(() -> service.delete(id));
    verify(promotionRepository).deleteById(id);
  }

  @Test
  void list_should_return_all_promotions() {
    when(promotionRepository.findAll())
        .thenReturn(
            List.of(
                JPromotion.builder().id(UUID.randomUUID()).year(2024).build(),
                JPromotion.builder().id(UUID.randomUUID()).year(2025).build()));

    var result = service.list();

    assertThat(result).hasSize(2);
    assertThat(result).extracting(PromotionResponse::year).containsExactlyInAnyOrder(2024, 2025);
  }

  @Test
  void get_should_return_a_promotion() {
    var id = UUID.randomUUID();
    when(promotionRepository.findById(id))
        .thenReturn(Optional.of(JPromotion.builder().id(id).year(2024).build()));

    var response = service.get(id);

    assertThat(response.year()).isEqualTo(2024);
    assertThat(response.id()).isEqualTo(id);
  }

  @Test
  void get_should_throw_for_unknown_id() {
    var id = UUID.randomUUID();
    when(promotionRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(id)).isInstanceOf(java.util.NoSuchElementException.class);
  }

  @Test
  void update_should_reject_duplicate_year_from_another_promotion() {
    var id = UUID.randomUUID();
    var otherId = UUID.randomUUID();
    when(promotionRepository.findById(id))
        .thenReturn(Optional.of(JPromotion.builder().id(id).year(2024).build()));
    when(promotionRepository.findByYear(2025))
        .thenReturn(Optional.of(JPromotion.builder().id(otherId).year(2025).build()));

    assertThatThrownBy(() -> service.update(id, new PromotionRequest(2025)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void update_should_reject_unknown_promotion() {
    var id = UUID.randomUUID();
    when(promotionRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.update(id, new PromotionRequest(2024)))
        .isInstanceOf(java.util.NoSuchElementException.class);
  }
}
