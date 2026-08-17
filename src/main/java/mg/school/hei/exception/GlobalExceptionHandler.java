package mg.school.hei.exception;

import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Object> handleBadRequest(IllegalArgumentException e) {
    return build(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<Object> handleNotFound(NoSuchElementException e) {
    return build(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Object> handleConflict(IllegalStateException e) {
    return build(HttpStatus.CONFLICT, e.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException e) {
    return build(HttpStatus.CONFLICT, "This operation violates a uniqueness or referential constraint");
  }

  private ResponseEntity<Object> build(HttpStatus status, String message) {
    return ResponseEntity.status(status)
            .body(Map.of("timestamp", Instant.now(), "status", status.value(), "message", message));
  }
}