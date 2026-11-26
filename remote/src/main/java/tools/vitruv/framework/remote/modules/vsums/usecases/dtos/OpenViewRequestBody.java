package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Request body for opening a view on a selection of objects of a VSUM.
 *
 * <p>Overrides {@code equals}, {@code hashCode} and {@code toString} so that they consider the
 * content of {@code selectedObjectIds} instead of the array reference.
 */
public record OpenViewRequestBody(UUID vsumId, UUID selectorId, UUID[] selectedObjectIds) {

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof OpenViewRequestBody(var otherVsum, var otherSelector, var otherIds))) {
      return false;
    }
    return Objects.equals(vsumId, otherVsum)
        && Objects.equals(selectorId, otherSelector)
        && Arrays.equals(selectedObjectIds, otherIds);
  }

  @Override
  public int hashCode() {
    return 31 * Objects.hash(vsumId, selectorId) + Arrays.hashCode(selectedObjectIds);
  }

  @Override
  public String toString() {
    return "OpenViewRequestBody[vsumId="
        + vsumId
        + ", selectorId="
        + selectorId
        + ", selectedObjectIds="
        + Arrays.toString(selectedObjectIds)
        + "]";
  }
}
