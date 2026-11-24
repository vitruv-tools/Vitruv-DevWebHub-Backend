package vitruv.tools.framework.remote.vsumprovider;

import java.nio.file.Path;
import tools.vitruv.change.interaction.InteractionResultProvider;

public interface VsumProvider {
  String getMetamodelName();

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
