package mg.school.hei.service.event;

import static java.io.File.createTempFile;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import mg.school.hei.endpoint.event.model.TranscriptPdfRequested;
import mg.school.hei.endpoint.rest.controller.dto.FullTranscriptResponse;
import mg.school.hei.endpoint.rest.controller.dto.TranscriptCourseLine;
import mg.school.hei.endpoint.rest.controller.dto.TranscriptResponse;
import mg.school.hei.file.bucket.BucketComponent;
import mg.school.hei.mail.Email;
import mg.school.hei.mail.Mailer;
import mg.school.hei.service.TranscriptService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranscriptPdfRequestedService implements Consumer<TranscriptPdfRequested> {

  private static final Duration LINK_EXPIRATION = Duration.ofDays(7);

  private final TranscriptService transcriptService;
  private final BucketComponent bucketComponent;
  private final Mailer mailer;

  @Override
  @SneakyThrows
  public void accept(TranscriptPdfRequested event) {
    UUID studentId = UUID.fromString(event.getStudentId());
    FullTranscriptResponse transcript = transcriptService.getFullTranscript(studentId);

    File pdfFile = generatePdf(transcript);
    String bucketKey = "transcripts/" + studentId + "/" + UUID.randomUUID() + ".pdf";
    bucketComponent.upload(pdfFile, bucketKey);

    var downloadUrl = bucketComponent.presign(bucketKey, LINK_EXPIRATION);
    sendEmail(event.getRecipientEmail(), transcript, downloadUrl.toString());

    log.info("Transcript PDF generated and emailed for studentId={}", studentId);
  }

  @SneakyThrows
  private File generatePdf(FullTranscriptResponse transcript) {
    File file = createTempFile("transcript-" + transcript.std(), ".pdf");
    Document document = new Document();
    PdfWriter.getInstance(document, new FileOutputStream(file));
    document.open();

    Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
    Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD);
    Font normalFont = new Font(Font.HELVETICA, 11, Font.NORMAL);

    document.add(new Paragraph("Academic Transcript", titleFont));
    document.add(new Paragraph("Student: " + transcript.std(), normalFont));
    document.add(new Paragraph(" "));

    for (TranscriptResponse year : transcript.years()) {
      document.add(new Paragraph("Academic Year " + year.academicYear(), sectionFont));

      PdfPTable table = new PdfPTable(4);
      table.setWidthPercentage(100);
      addHeaderCell(table, "Course");
      addHeaderCell(table, "Title");
      addHeaderCell(table, "Average");
      addHeaderCell(table, "Credits");

      for (TranscriptCourseLine course : year.courses()) {
        table.addCell(new PdfPCell(new Paragraph(course.courseRef(), normalFont)));
        table.addCell(new PdfPCell(new Paragraph(course.courseTitle(), normalFont)));
        table.addCell(
            new PdfPCell(
                new Paragraph(
                    course.average() != null ? String.valueOf(course.average()) : "N/A",
                    normalFont)));
        table.addCell(new PdfPCell(new Paragraph(String.valueOf(course.credits()), normalFont)));
      }

      document.add(table);
      document.add(
          new Paragraph(
              "Year average: " + year.generalAverage() + " | Credits: " + year.totalCredits(),
              normalFont));
      document.add(new Paragraph(" "));
    }

    document.add(new Paragraph(" "));
    document.add(
        new Paragraph("Cumulative average: " + transcript.cumulativeGeneralAverage(), sectionFont));
    document.add(
        new Paragraph("Cumulative credits: " + transcript.cumulativeCredits(), sectionFont));

    document.close();
    return file;
  }

  private void addHeaderCell(PdfPTable table, String text) {
    PdfPCell cell = new PdfPCell(new Paragraph(text, new Font(Font.HELVETICA, 11, Font.BOLD)));
    table.addCell(cell);
  }

  @SneakyThrows
  private void sendEmail(String recipientEmail, FullTranscriptResponse transcript, String url) {
    String html =
        "<p>Hello,</p>"
            + "<p>Your academic transcript (student "
            + transcript.std()
            + ") is ready.</p>"
            + "<p><a href=\""
            + url
            + "\">Download your transcript (PDF)</a></p>"
            + "<p>This link expires in "
            + LINK_EXPIRATION.toDays()
            + " days.</p>";

    Email email =
        new Email(
            new InternetAddress(recipientEmail),
            List.of(),
            List.of(),
            "Your academic transcript is ready",
            html,
            List.of());

    mailer.accept(email);
  }
}
