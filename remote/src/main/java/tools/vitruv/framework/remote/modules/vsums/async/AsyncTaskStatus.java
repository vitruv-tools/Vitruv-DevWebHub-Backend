package tools.vitruv.framework.remote.modules.vsums.async;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import tools.vitruv.change.interaction.UserInteractionBase;

/**
 * Mutable status of one asynchronous propagation task, including a pending user interaction
 * and the response handed back to the waiting worker thread.
 */
@Getter
public class AsyncTaskStatus {
  private final UUID taskId;
  private final UUID viewId;
  private volatile AsyncTaskState state;
  private final Instant createdAt;
  private Instant completedAt;
  private String result;
  private String errorMessage;
  private volatile UserInteractionBase pendingInteraction;
  private volatile UserInteractionBase interactionResponse;
  private volatile String validationError;

  /** Resource set committed at the start of async propagation (includes user edits). */
  private volatile String committedResourceSet;

  /**
   * True when the editor dismissed the dialog; worker stays waiting so the hub can answer later.
   */
  private volatile boolean parkedAsInconsistency;

  private final Object interactionLock = new Object();

  /** Creates a status for a new task in state {@link AsyncTaskState#RUNNING}. */
  public AsyncTaskStatus(UUID taskId, UUID viewId) {
    this.taskId = taskId;
    this.viewId = viewId;
    this.state = AsyncTaskState.RUNNING;
    this.createdAt = Instant.now();
  }

  /** Marks the task as {@link AsyncTaskState#COMPLETED} with the given result. */
  public void complete(String result) {
    this.state = AsyncTaskState.COMPLETED;
    this.result = result;
    this.completedAt = Instant.now();
    this.validationError = null;
  }

  /** Marks the task as {@link AsyncTaskState#FAILED} with the given error message. */
  public void fail(String errorMessage) {
    this.state = AsyncTaskState.FAILED;
    this.errorMessage = errorMessage;
    this.completedAt = Instant.now();
    this.validationError = null;
  }

  /**
   * Parks a waiting interaction as an inconsistency without aborting the worker. The task stays
   * {@link AsyncTaskState#WAITING_USER_INTERACTION} so the hub can submit an answer later.
   */
  public void parkAsInconsistency() {
    synchronized (interactionLock) {
      if (state != AsyncTaskState.WAITING_USER_INTERACTION) {
        return;
      }
      this.parkedAsInconsistency = true;
      this.validationError = null;
    }
  }

  /**
   * Marks the task as inconsistent and completes it.
   *
   * @deprecated Prefer {@link #parkAsInconsistency()} so the hub can still resolve the same task.
   *     Kept for abort-style tests; wakes the worker and marks the task completed as {@code
   *     INCONSISTENT}.
   */
  @Deprecated
  public void markInconsistent() {
    synchronized (interactionLock) {
      this.state = AsyncTaskState.INCONSISTENT;
      this.parkedAsInconsistency = true;
      this.completedAt = Instant.now();
      this.validationError = null;
      this.interactionResponse = null;
      interactionLock.notifyAll();
    }
  }

  public boolean isParkedAsInconsistency() {
    return parkedAsInconsistency;
  }

  public void setCommittedResourceSet(String committedResourceSet) {
    this.committedResourceSet = committedResourceSet;
  }

  /**
   * Registers the given interaction as pending and switches the task to {@link
   * AsyncTaskState#WAITING_USER_INTERACTION}.
   */
  public void setWaitingForInteraction(UserInteractionBase interaction) {
    this.pendingInteraction = interaction;
    this.state = AsyncTaskState.WAITING_USER_INTERACTION;
  }

  public void setValidationError(String validationError) {
    this.validationError = validationError;
  }

  /** Clears the validation error of the last interaction response. */
  public void clearValidationError() {
    this.validationError = null;
  }

  /**
   * Blocks the calling worker until a response is submitted or the task is marked inconsistent,
   * then switches the task back to {@link AsyncTaskState#RUNNING}.
   *
   * @param <T> the expected interaction type of the response
   * @return the submitted interaction response
   * @throws InterruptedException if the waiting thread is interrupted
   * @throws PropagationInconsistentException if the task was marked inconsistent while
   *     waiting
   */
  @SuppressWarnings("unchecked")
  public <T extends UserInteractionBase> T waitForInteractionResponse()
      throws InterruptedException {
    synchronized (interactionLock) {
      while (interactionResponse == null && state == AsyncTaskState.WAITING_USER_INTERACTION) {
        interactionLock.wait();
      }
      if (state == AsyncTaskState.INCONSISTENT) {
        throw new PropagationInconsistentException(taskId);
      }
      final T response = (T) interactionResponse;
      interactionResponse = null;
      pendingInteraction = null;
      this.state = AsyncTaskState.RUNNING;
      return response;
    }
  }

  /**
   * Hands the answer to the waiting worker. The state switches to {@link AsyncTaskState#RUNNING}
   * immediately, so pollers never see the already-answered prompt as still pending (e.g. without a
   * validation error that the worker sets before re-prompting).
   */
  public void setInteractionResponse(UserInteractionBase response) {
    synchronized (interactionLock) {
      this.interactionResponse = response;
      this.state = AsyncTaskState.RUNNING;
      interactionLock.notifyAll();
    }
  }

  public boolean isWaitingForUserInteraction() {
    return state == AsyncTaskState.WAITING_USER_INTERACTION;
  }

  public boolean isCompleted() {
    return state == AsyncTaskState.COMPLETED
        || state == AsyncTaskState.FAILED
        || state == AsyncTaskState.INCONSISTENT;
  }
}
