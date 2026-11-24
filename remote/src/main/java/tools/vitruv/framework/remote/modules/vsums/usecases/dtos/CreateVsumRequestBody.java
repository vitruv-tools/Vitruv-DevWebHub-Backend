package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

/** Request body for creating a VSUM. */
public record CreateVsumRequestBody(String metamodelName, String name, String description) {}
