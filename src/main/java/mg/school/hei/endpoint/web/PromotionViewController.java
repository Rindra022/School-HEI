package mg.school.hei.endpoint.web;

import lombok.RequiredArgsConstructor;
import mg.school.hei.service.PromotionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PromotionViewController {
  private final PromotionService promotionService;

  @GetMapping("/promotions-view")
  public String listPromotions(Model model) {
    model.addAttribute("promotions", promotionService.list());
    return "promotions";
  }
}
