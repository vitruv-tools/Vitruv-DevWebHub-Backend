package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

import java.util.UUID;

/** Request body for opening a view on a selection of objects of a VSUM. */
public record OpenViewRequestBody(UUID vsumId, UUID selectorId, UUID[] selectedObjectIds) {}
