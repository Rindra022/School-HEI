package mg.school.hei.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mg.school.hei.endpoint.rest.controller.dto.GraduateResponse;
import mg.school.hei.file.bucket.BucketComponent;
import mg.school.hei.model.Track;
import mg.school.hei.repository.PromotionRepository;
import mg.school.hei.repository.model.JPromotion;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class GraduateExportServiceTest {

  private final PromotionRepository promotionRepository = mock(PromotionRepository.class);
  private final GraduateService graduateService = mock(GraduateService.class);
  private final BucketComponent bucketComponent = mock(BucketComponent.class);

  private final GraduateExportService service =
      new GraduateExportService(promotionRepository, graduateService, bucketComponent);

  @Test
  void export_should_reject_an_unknown_promotion() {
    var promotionId = UUID.randomUUID();
    when(promotionRepository.findById(promotionId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.exportGraduatesXlsx(promotionId))
        .isInstanceOf(java.util.NoSuchElementException.class);
    verifyNoInteractions(bucketComponent);
  }

  @Test
  void export_should_upload_a_workbook_with_el_and_tn_sheets_and_return_the_presigned_url()
      throws Exception {
    var promotionId = UUID.randomUUID();
    when(promotionRepository.findById(promotionId))
        .thenReturn(Optional.of(JPromotion.builder().id(promotionId).year(2024).build()));

    when(graduateService.listGraduates(promotionId, Track.EL))
        .thenReturn(
            List.of(
                new GraduateResponse(1, "STD24001", "Jean", "Rakoto", 15.5, Track.EL),
                new GraduateResponse(2, "STD24002", "Marie", "Rabe", 12.25, Track.EL)));
    when(graduateService.listGraduates(promotionId, Track.TN)).thenReturn(List.of());

    var capturedFile = new File[1];
    when(bucketComponent.upload(any(File.class), anyString()))
        .thenAnswer(
            inv -> {
              capturedFile[0] = inv.getArgument(0);
              return null;
            });
    var expectedUrl = new URL("https://bucket.example.com/graduates.xlsx?signed=1");
    when(bucketComponent.presign(anyString(), any(Duration.class))).thenReturn(expectedUrl);

    var result = service.exportGraduatesXlsx(promotionId);

    assertThat(result).isEqualTo(expectedUrl);
    verify(bucketComponent).upload(any(File.class), anyString());
    verify(bucketComponent).presign(anyString(), any(Duration.class));

    try (XSSFWorkbook workbook = new XSSFWorkbook(capturedFile[0])) {
      Sheet elSheet = workbook.getSheet("EL");
      assertThat(elSheet).isNotNull();

      Row header = elSheet.getRow(0);
      assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Rang");
      assertThat(header.getCell(1).getStringCellValue()).isEqualTo("STD");
      assertThat(header.getCell(2).getStringCellValue()).isEqualTo("Nom");
      assertThat(header.getCell(3).getStringCellValue()).isEqualTo("Prénom");
      assertThat(header.getCell(4).getStringCellValue()).isEqualTo("Moyenne générale");

      Row firstGraduate = elSheet.getRow(1);
      assertThat(firstGraduate.getCell(0).getNumericCellValue()).isEqualTo(1.0);
      assertThat(firstGraduate.getCell(1).getStringCellValue()).isEqualTo("STD24001");
      assertThat(firstGraduate.getCell(2).getStringCellValue()).isEqualTo("Rakoto");
      assertThat(firstGraduate.getCell(3).getStringCellValue()).isEqualTo("Jean");
      assertThat(firstGraduate.getCell(4).getNumericCellValue()).isEqualTo(15.5);

      Sheet tnSheet = workbook.getSheet("TN");
      assertThat(tnSheet).isNotNull();
      assertThat(tnSheet.getLastRowNum()).isZero();
    }
  }
}
