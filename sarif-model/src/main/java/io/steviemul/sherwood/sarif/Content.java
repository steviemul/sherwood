package io.steviemul.sherwood.sarif;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Generated;

@Generated("jsonschema2pojo")
public enum Content {
  LOCALIZED_DATA("localizedData"),
  NON_LOCALIZED_DATA("nonLocalizedData");
  private final String value;
  private static final Map<String, Content> CONSTANTS = new HashMap<String, Content>();

  static {
    for (Content c : values()) {
      CONSTANTS.put(c.value, c);
    }
  }

  Content(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return this.value;
  }

  @JsonValue
  public String value() {
    return this.value;
  }

  @JsonCreator
  public static Content fromValue(String value) {
    Content constant = CONSTANTS.get(value);
    if (constant == null) {
      throw new IllegalArgumentException(value);
    } else {
      return constant;
    }
  }
}
