package tools.vitruv.framework.remote.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for change propagation bound from {@code app.propagation}.
 *
 * @param corePoolSize core thread count of the propagation executor
 * @param maxPoolSize maximum thread count of the propagation executor
 * @param queueCapacity capacity of the executor's task queue
 * @param taskTtlHours time to live of propagation tasks in hours
 */
@ConfigurationProperties(prefix = "app.propagation")
public record PropagationProperties(
    int corePoolSize, int maxPoolSize, int queueCapacity, int taskTtlHours) {
  /** Replaces non-positive values with the defaults 2, 4, 50, and 24, respectively. */
  public PropagationProperties {
    if (corePoolSize <= 0) {
      corePoolSize = 2;
    }
    if (maxPoolSize <= 0) {
      maxPoolSize = 4;
    }
    if (queueCapacity <= 0) {
      queueCapacity = 50;
    }
    if (taskTtlHours <= 0) {
      taskTtlHours = 24;
    }
  }
}
