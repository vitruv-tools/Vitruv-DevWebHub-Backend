package vitruv.tools.framework.remote.vsumprovider;

import java.nio.file.Path;
import tools.vitruv.change.interaction.InteractionResultProvider;

/** Provides a VSUM (virtual single underlying model) and its initializer. */
public interface VsumProvider {
  /** Returns the name under which this VSUM is identified. */
  String getMetamodelName();

  /** Returns an initializer that creates the virtual model in the given storage folder. */
  VirtualModelInitializer getInitializer(Path storagePath);

  /**
   * Server-side initialization with a caller-supplied interaction provider (e.g. {@code
   * VitruviusInteractionResultProvider} on VitruviusServer).
   */
  default VirtualModelInitializer getInitializer(
      Path storagePath, InteractionResultProvider interactionResultProvider) {
    return getInitializer(storagePath);
  }
}
