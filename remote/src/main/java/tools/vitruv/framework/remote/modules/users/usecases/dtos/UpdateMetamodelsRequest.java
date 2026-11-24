package tools.vitruv.framework.remote.modules.users.usecases.dtos;

import java.util.List;

/**
 * Request to replace a user's known metamodels.
 *
 * @param metamodels the names of the metamodels
 */
public record UpdateMetamodelsRequest(List<String> metamodels) {}
