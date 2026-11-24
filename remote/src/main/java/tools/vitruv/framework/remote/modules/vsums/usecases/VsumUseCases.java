package tools.vitruv.framework.remote.modules.vsums.usecases;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.vitruv.framework.remote.modules.vsums.usecases.strategies.UseCasesStrategySelector;

/** VSUM use cases that delegate to the strategy selected for the given VSUM. */
@Component
@RequiredArgsConstructor
public class VsumUseCases {
  private final UseCasesStrategySelector useCasesStrategySelector;

  /** Returns the names of the view types of the given VSUM. */
  public String[] getViewTypeNames(UUID vsumId) {
    return useCasesStrategySelector.getVsumStrategy(vsumId).getViewTypes(vsumId);
  }

  /** Deletes the given VSUM. */
  public void deleteVsum(UUID vsumId) {
    useCasesStrategySelector.getVsumStrategy(vsumId).deleteVsum(vsumId);
  }
}
