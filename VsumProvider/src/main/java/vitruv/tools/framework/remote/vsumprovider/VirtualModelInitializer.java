package vitruv.tools.framework.remote.vsumprovider;

import tools.vitruv.framework.vsum.VirtualModel;

/** Creates and initializes a {@link VirtualModel} on demand. */
@FunctionalInterface
public interface VirtualModelInitializer {
  /**
   * Creates and initializes the virtual model.
   *
   * @return the initialized virtual model
   */
  VirtualModel init();
}
