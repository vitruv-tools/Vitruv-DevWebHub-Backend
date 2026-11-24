package tools.vitruv.framework.remote.modules.vsums.async;

import java.util.UUID;

/**
 * Thrown when a waiting user interaction is dismissed and the task is marked {@link
 * AsyncTaskState#INCONSISTENT}. Unblocks the propagation worker so the VSUM/view can be used again.
 */
public class PropagationInconsistentException extends RuntimeException {
  /**
   * Creates the exception for the given task.
   *
   * @param taskId the ID of the halted task
   */
  public PropagationInconsistentException(UUID taskId) {
    super("Propagation halted as inconsistent for task " + taskId);
  }
}
