package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

/** Request body for updating the name and description of a VSUM. */
public record UpdateVsumInfoRequestBody(String name, String description) {}
