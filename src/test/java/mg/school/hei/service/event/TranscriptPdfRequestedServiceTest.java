package mg.school.hei.service.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import mg.school.hei.endpoint.event.model.TranscriptPdfRequested;
import mg.school.hei.endpoint.rest.controller.dto.FullTranscriptResponse;
import mg.school.hei.endpoint.rest.controller.dto.TranscriptCourseLine;
import mg.school.hei.endpoint.rest.controller.dto.TranscriptResponse;
import mg.school.hei.file.bucket.BucketComponent;
import mg.school.hei.mail.Email;
import mg.school.hei.mail.Mailer;
import mg.school.hei.service.TranscriptService;
import org.junit.jupiter.api.Test;

class TranscriptPdfRequestedServiceTest {

  private final TranscriptService transcriptService = mock(TranscriptService.class);
  private final BucketComponent bucketComponent = mock(BucketComponent.class);
  private final Mailer mailer = mock(Mailer.class);

  private final TranscriptPdfRequestedService service =
      new TranscriptPdfRequestedService(transcriptService, bucketComponent, mailer);

  @Test
  void accept_should_generate_pdf_upload_it_and_send_email() throws Exception {
    var studentId = UUID.randomUUID();
    var course = new TranscriptCourseLine("PROG4", "Qualite", 15.0, 6, true);
    var year = new TranscriptResponse(studentId, "STD24001", 2024, List.of(course), 15.0, 6, true);
    var transcript =
        new FullTranscriptResponse(studentId, "STD24001", List.of(year), 15.0, 6, true);

    when(transcriptService.getFullTranscript(studentId)).thenReturn(transcript);
    when(bucketComponent.upload(any(File.class), anyString())).thenReturn(null);
    when(bucketComponent.presign(anyString(), any(Duration.class)))
        .thenReturn(new URL("https://bucket.example.com/transcript.pdf?signed=1"));

    var event =
        TranscriptPdfRequested.builder()
            .studentId(studentId.toString())
            .recipientEmail("student@hei.school")
            .build();

    service.accept(event);

    verify(bucketComponent).upload(any(File.class), anyString());
    verify(bucketComponent).presign(anyString(), any(Duration.class));
    verify(mailer).accept(any(Email.class));
  }
}
