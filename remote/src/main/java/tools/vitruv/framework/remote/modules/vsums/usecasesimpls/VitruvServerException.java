package tools.vitruv.framework.remote.modules.vsums.usecasesimpls;

/** Thrown when the remote Vitruv server answers a request with a non-success status code. */
public class VitruvServerException extends RuntimeException {

  /**
   * Creates an exception with the given message.
   *
   * @param message the detail message, including the status code and response body
   */
  public VitruvServerException(String message) {
    super(message);
  }
}
