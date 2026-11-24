package tools.vitruv.framework.remote.modules.vsums.async;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** In-memory registry of asynchronous propagation tasks and their status, keyed by task ID. */
@Component
public class PropagationTaskRegistry {
  private final Map<UUID, AsyncTaskStatus> tasks = new ConcurrentHashMap<>();

  /**
   * Registers a new running task for the given view.
   *
   * @return the status object of the new task
   */
  public AsyncTaskStatus register(UUID taskId, UUID viewId) {
    AsyncTaskStatus status = new AsyncTaskStatus(taskId, viewId);
    tasks.put(taskId, status);
    return status;
  }

  /** Returns the status of the given task, or {@code null} if it is not registered. */
  public AsyncTaskStatus getTaskStatus(UUID taskId) {
    return tasks.get(taskId);
  }

  /** Marks the given task as completed with the given result, if it is registered. */
  public void completeTask(UUID taskId, String result) {
    AsyncTaskStatus status = tasks.get(taskId);
    if (status != null) {
      status.complete(result);
    }
  }

  /** Marks the given task as failed with the given error message, if it is registered. */
  public void failTask(UUID taskId, String errorMessage) {
    AsyncTaskStatus status = tasks.get(taskId);
    if (status != null) {
      status.fail(errorMessage);
    }
  }

  /**
   * Parks the waiting interaction of the given task as an inconsistency, if it is registered.
   *
   * @see AsyncTaskStatus#parkAsInconsistency()
   */
  public void parkAsInconsistency(UUID taskId) {
    AsyncTaskStatus status = tasks.get(taskId);
    if (status != null) {
      status.parkAsInconsistency();
    }
  }

  /**
   * Marks the task with the given ID as inconsistent and completes it.
   *
   * @deprecated abort-style park; prefer {@link #parkAsInconsistency(UUID)}
   */
  @Deprecated
  public void markInconsistent(UUID taskId) {
    AsyncTaskStatus status = tasks.get(taskId);
    if (status != null) {
      status.markInconsistent();
    }
  }

  /** Removes the given task from the registry. */
  public void removeTask(UUID taskId) {
    tasks.remove(taskId);
  }

  /** Returns whether a task with the given ID is registered. */
  public boolean taskExists(UUID taskId) {
    return tasks.containsKey(taskId);
  }

  /** Returns whether any registered task is running or waiting for a user interaction. */
  public boolean hasActivePropagationTask() {
    return tasks.values().stream().anyMatch(this::isActive);
  }

  /**
   * Resolves the task id for an in-flight async propagation. Uses the worker thread context when
   * available, otherwise the sole active task.
   */
  public Optional<UUID> resolveActiveTaskId() {
    Optional<UUID> taskIdFromThread = ServerInteractionResultProvider.getCurrentTaskId();
    if (taskIdFromThread.isPresent()) {
      return taskIdFromThread;
    }

    List<AsyncTaskStatus> activeTasks = tasks.values().stream().filter(this::isActive).toList();
    if (activeTasks.size() == 1) {
      return Optional.of(activeTasks.get(0).getTaskId());
    }
    return Optional.empty();
  }

  private boolean isActive(AsyncTaskStatus status) {
    return status.getState() == AsyncTaskState.RUNNING
        || status.getState() == AsyncTaskState.WAITING_USER_INTERACTION;
  }
}
