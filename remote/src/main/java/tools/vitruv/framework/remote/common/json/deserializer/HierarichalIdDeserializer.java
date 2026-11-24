package tools.vitruv.framework.remote.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emfcloud.jackson.databind.EMFContext;
import org.eclipse.emfcloud.jackson.databind.deser.EcoreReferenceDeserializer;
import org.eclipse.emfcloud.jackson.databind.deser.ReferenceEntry;
import tools.vitruv.framework.remote.common.json.IdTransformation;

/**
 * Deserializes references given as hierarchical ID strings into global references, delegating
 * all other representations to the standard EMF reference deserializer.
 */
public class HierarichalIdDeserializer extends JsonDeserializer<ReferenceEntry> {
  private final EcoreReferenceDeserializer standardDeserializer;
  private final IdTransformation transformation;

  /**
   * Creates a deserializer that wraps the given standard deserializer.
   *
   * @param standardDeserializer the deserializer used for non-string references
   * @param transformation the transformation from hierarchical IDs to global URIs
   */
  public HierarichalIdDeserializer(
      EcoreReferenceDeserializer standardDeserializer, IdTransformation transformation) {
    this.standardDeserializer = standardDeserializer;
    this.transformation = transformation;
  }

  @Override
  public ReferenceEntry deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    if (parser.currentToken() == JsonToken.VALUE_STRING) {
      var node = context.readTree(parser);
      return new HidReferenceEntry(
          EMFContext.getParent(context),
          EMFContext.getReference(context),
          transformation.toGlobal(URI.createURI(node.asText())).toString());
    }
    return standardDeserializer.deserialize(parser, context);
  }
}
