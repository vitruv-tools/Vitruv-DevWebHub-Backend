package tools.vitruv.framework.remote.modules.vsums.usecases;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.vitruv.framework.remote.modules.vsums.usecases.strategies.UseCasesStrategySelector;

@Component
@RequiredArgsConstructor
public class VsumUseCases {
  private final UseCasesStrategySelector useCasesStrategySelector;

  public String[] getViewTypeNames(UUID vsumId) {
    return useCasesStrategySelector.getVsumStrategy(vsumId).getViewTypes(vsumId);
  }

  public void deleteVsum(UUID vsumId) {
    useCasesStrategySelector.getVsumStrategy(vsumId).deleteVsum(vsumId);
  }
}
