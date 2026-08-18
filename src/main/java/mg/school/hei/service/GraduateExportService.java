package mg.school.hei.service;

import static java.io.File.createTempFile;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import mg.school.hei.endpoint.rest.controller.dto.GraduateResponse;
import mg.school.hei.file.bucket.BucketComponent;
import mg.school.hei.model.Track;
import mg.school.hei.repository.PromotionRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GraduateExportService {

  private static final Duration DOWNLOAD_LINK_EXPIRATION = Duration.ofMinutes(15);
  private static final String[] HEADERS = {"Rang", "STD", "Nom", "Prénom", "Moyenne générale"};

  private final PromotionRepository promotionRepository;
  private final GraduateService graduateService;
  private final BucketComponent bucketComponent;

  public URL exportGraduatesXlsx(UUID promotionId) {
    var promotion =
        promotionRepository
            .findById(promotionId)
            .orElseThrow(() -> new NoSuchElementException("Promotion not found"));

    File file = generateWorkbook(promotionId, promotion.getYear());
    String bucketKey = "graduates/" + promotionId + "/" + UUID.randomUUID() + ".xlsx";
    bucketComponent.upload(file, bucketKey);

    return bucketComponent.presign(bucketKey, DOWNLOAD_LINK_EXPIRATION);
  }

  @SneakyThrows
  private File generateWorkbook(UUID promotionId, Integer year) {
    File file = createTempFile("graduates-" + year + "-", ".xlsx");

    try (XSSFWorkbook workbook = new XSSFWorkbook();
        FileOutputStream out = new FileOutputStream(file)) {
      addSheet(workbook, "EL", graduateService.listGraduates(promotionId, Track.EL));
      addSheet(workbook, "TN", graduateService.listGraduates(promotionId, Track.TN));
      workbook.write(out);
    }

    return file;
  }

  private void addSheet(XSSFWorkbook workbook, String sheetName, List<GraduateResponse> graduates) {
    Sheet sheet = workbook.createSheet(sheetName);

    Row headerRow = sheet.createRow(0);
    for (int i = 0; i < HEADERS.length; i++) {
      headerRow.createCell(i).setCellValue(HEADERS[i]);
    }

    int rowIndex = 1;
    for (GraduateResponse graduate : graduates) {
      Row row = sheet.createRow(rowIndex++);
      row.createCell(0).setCellValue(graduate.rank());
      row.createCell(1).setCellValue(graduate.std());
      row.createCell(2).setCellValue(graduate.lastName());
      row.createCell(3).setCellValue(graduate.firstName());
      row.createCell(4).setCellValue(graduate.generalAverage());
    }
  }
}
