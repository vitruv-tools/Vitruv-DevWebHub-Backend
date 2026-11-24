package tools.vitruv.framework.remote.modules.vsums.async;

/** Lifecycle states of an asynchronous propagation task. */
public enum AsyncTaskState {
  RUNNING,
  WAITING_USER_INTERACTION,
  COMPLETED,
  FAILED,
  /**
   * User dismissed a required interaction; worker was unblocked and the task is kept as an
   * inconsistency.
   */
  INCONSISTENT
}
