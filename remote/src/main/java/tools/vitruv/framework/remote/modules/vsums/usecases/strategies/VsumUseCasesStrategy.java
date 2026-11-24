package tools.vitruv.framework.remote.modules.vsums.usecases.strategies;

import java.util.UUID;

/** Strategy for VSUM-level use cases, such as listing view types and deleting a VSUM. */
public interface VsumUseCasesStrategy {
  /** Returns the names of the view types of the given VSUM. */
  String[] getViewTypes(UUID vsumId);

  /** Deletes the given VSUM. */
  void deleteVsum(UUID vsumId);
}
