package mg.school.hei.exception;

public class ResourceNotFoundException extends IllegalArgumentException {
  public ResourceNotFoundException(String message) {
    super(message);
  }
}
