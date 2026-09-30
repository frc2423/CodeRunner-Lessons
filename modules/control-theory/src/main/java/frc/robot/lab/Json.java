package frc.robot.lab;

import java.math.BigDecimal;
import java.math.MathContext;

/** Just enough JSON writing for the lab's messages to the dashboard. */
final class Json {
  private static final MathContext SIGNIFICANT = new MathContext(6);

  private final StringBuilder out = new StringBuilder();
  private boolean needComma = false;

  Json beginObject() {
    comma();
    out.append('{');
    needComma = false;
    return this;
  }

  Json endObject() {
    out.append('}');
    needComma = true;
    return this;
  }

  Json beginArray() {
    comma();
    out.append('[');
    needComma = false;
    return this;
  }

  Json endArray() {
    out.append(']');
    needComma = true;
    return this;
  }

  Json key(String key) {
    comma();
    string(key);
    out.append(':');
    needComma = false;
    return this;
  }

  Json value(String value) {
    comma();
    string(value);
    needComma = true;
    return this;
  }

  Json value(boolean value) {
    comma();
    out.append(value);
    needComma = true;
    return this;
  }

  /** A number, or null for NaN and infinity (which JSON can't hold). */
  Json value(double value) {
    comma();
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      out.append("null");
    } else if (value == Math.rint(value) && Math.abs(value) < 1e15) {
      out.append((long) value);
    } else {
      out.append(new BigDecimal(value).round(SIGNIFICANT).stripTrailingZeros().toString());
    }
    needComma = true;
    return this;
  }

  Json array(double[] values) {
    beginArray();
    for (double value : values) {
      value(value);
    }
    return endArray();
  }

  private void comma() {
    if (needComma) {
      out.append(',');
    }
  }

  private void string(String value) {
    out.append('"');
    for (char c : value.toCharArray()) {
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> {
          if (c < 0x20) {
            out.append(String.format("\\u%04x", (int) c));
          } else {
            out.append(c);
          }
        }
      }
    }
    out.append('"');
  }

  @Override
  public String toString() {
    return out.toString();
  }
}
