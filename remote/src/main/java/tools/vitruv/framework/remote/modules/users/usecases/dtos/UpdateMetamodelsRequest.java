package tools.vitruv.framework.remote.modules.users.usecases.dtos;

import java.util.List;

public record UpdateMetamodelsRequest(
    List<String> metamodels
) {
}
