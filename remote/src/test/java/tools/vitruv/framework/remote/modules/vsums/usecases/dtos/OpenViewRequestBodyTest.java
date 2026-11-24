package tools.vitruv.framework.remote.modules.vsums.usecases.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class OpenViewRequestBodyTest {

  private static final UUID VSUM_ID = UUID.randomUUID();
  private static final UUID SELECTOR_ID = UUID.randomUUID();
  private static final UUID OBJECT_ID = UUID.randomUUID();

  @Test
  void equalsAndHashCodeCompareArrayContent() {
    var first = new OpenViewRequestBody(VSUM_ID, SELECTOR_ID, new UUID[] {OBJECT_ID});
    var second = new OpenViewRequestBody(VSUM_ID, SELECTOR_ID, new UUID[] {OBJECT_ID});

    assertThat(first).isEqualTo(second);
    assertThat(first).hasSameHashCodeAs(second);
  }

  @Test
  void equalsDetectsDifferentArrayContent() {
    var first = new OpenViewRequestBody(VSUM_ID, SELECTOR_ID, new UUID[] {OBJECT_ID});
    var second = new OpenViewRequestBody(VSUM_ID, SELECTOR_ID, new UUID[] {UUID.randomUUID()});

    assertThat(first).isNotEqualTo(second);
  }

  @Test
  void toStringContainsArrayContent() {
    var body = new OpenViewRequestBody(VSUM_ID, SELECTOR_ID, new UUID[] {OBJECT_ID});

    assertThat(body.toString()).contains("selectedObjectIds=[" + OBJECT_ID + "]");
  }
}
