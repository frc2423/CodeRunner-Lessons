package frc.robot.subsystems.LEDS;

import edu.wpi.first.wpilibj.AddressableLEDBuffer;
import edu.wpi.first.wpilibj.util.Color;

/**
 * The LED strip your challenge draws on: a row of LEDs numbered from 0 to {@code length() - 1}.
 *
 * <p>Every LED has a red, a green and a blue part, each from 0 (off) to 255 (full brightness).
 * Mixing them makes every other color: red + green = yellow, red + blue = magenta, all three =
 * white.
 *
 * <p>You don't need to edit this file.
 */
public class LedStrip {
  private final AddressableLEDBuffer buffer;

  LedStrip(AddressableLEDBuffer buffer) {
    this.buffer = buffer;
  }

  /** How many LEDs the strip has. The last LED's index is {@code length() - 1}. */
  public int length() {
    return buffer.getLength();
  }

  /**
   * Sets one LED's color from its red, green and blue parts.
   *
   * <pre>{@code
   * leds.setRGB(0, 255, 0, 0); // LED 0 bright red
   * leds.setRGB(1, 255, 255, 0); // LED 1 yellow
   * }</pre>
   *
   * @param index which LED, from 0 to {@code length() - 1}
   * @param red 0 to 255
   * @param green 0 to 255
   * @param blue 0 to 255
   */
  public void setRGB(int index, int red, int green, int blue) {
    checkIndex(index);
    checkChannel("red", red);
    checkChannel("green", green);
    checkChannel("blue", blue);
    buffer.setRGB(index, red, green, blue);
  }

  /**
   * Sets one LED to a {@link Color}, such as {@code Color.kRed} or {@code Color.kOrange}.
   *
   * @param index which LED, from 0 to {@code length() - 1}
   * @param color the color
   */
  public void setColor(int index, Color color) {
    if (color == null) {
      throw new IllegalArgumentException("The color for LED " + index + " is null.");
    }
    setRGB(
        index,
        (int) Math.round(color.red * 255),
        (int) Math.round(color.green * 255),
        (int) Math.round(color.blue * 255));
  }

  /**
   * Sets one LED's color by hue, saturation and value (brightness). Handy for rainbows.
   *
   * @param index which LED, from 0 to {@code length() - 1}
   * @param hue the color around the color wheel, in degrees: 0 red, 60 yellow, 120 green, 180 cyan,
   *     240 blue, 300 magenta. It wraps around, so 360 is red again and 400 is the same as 40.
   * @param saturation 0 (white) to 255 (full color)
   * @param value brightness, 0 (off) to 255 (full)
   */
  public void setHSV(int index, int hue, int saturation, int value) {
    checkIndex(index);
    checkChannel("saturation", saturation);
    checkChannel("value", value);

    double h = (((hue % 360) + 360) % 360) / 60.0;
    double v = value / 255.0;
    double chroma = v * (saturation / 255.0);
    double x = chroma * (1 - Math.abs(h % 2 - 1));
    double r;
    double g;
    double b;
    switch ((int) h) {
      case 0 -> {
        r = chroma;
        g = x;
        b = 0;
      }
      case 1 -> {
        r = x;
        g = chroma;
        b = 0;
      }
      case 2 -> {
        r = 0;
        g = chroma;
        b = x;
      }
      case 3 -> {
        r = 0;
        g = x;
        b = chroma;
      }
      case 4 -> {
        r = x;
        g = 0;
        b = chroma;
      }
      default -> {
        r = chroma;
        g = 0;
        b = x;
      }
    }
    double m = v - chroma;
    buffer.setRGB(
        index,
        (int) Math.round((r + m) * 255),
        (int) Math.round((g + m) * 255),
        (int) Math.round((b + m) * 255));
  }

  /** How much red one LED has, from 0 to 255. */
  public int getRed(int index) {
    checkIndex(index);
    return buffer.getRed(index);
  }

  /** How much green one LED has, from 0 to 255. */
  public int getGreen(int index) {
    checkIndex(index);
    return buffer.getGreen(index);
  }

  /** How much blue one LED has, from 0 to 255. */
  public int getBlue(int index) {
    checkIndex(index);
    return buffer.getBlue(index);
  }

  /** One LED's current color. */
  public Color getColor(int index) {
    return new Color(getRed(index), getGreen(index), getBlue(index));
  }

  /** Turns every LED off. */
  public void clear() {
    for (int i = 0; i < length(); i++) {
      buffer.setRGB(i, 0, 0, 0);
    }
  }

  /** Every LED's color packed as 0xRRGGBB, for the dashboard. */
  long[] toPackedArray() {
    long[] packed = new long[length()];
    for (int i = 0; i < packed.length; i++) {
      packed[i] = ((long) buffer.getRed(i) << 16) | (buffer.getGreen(i) << 8) | buffer.getBlue(i);
    }
    return packed;
  }

  private void checkIndex(int index) {
    if (index < 0 || index >= length()) {
      throw new IndexOutOfBoundsException(
          "There is no LED "
              + index
              + ". This strip has LEDs 0 to "
              + (length() - 1)
              + " (counting starts at 0).");
    }
  }

  private static void checkChannel(String name, int amount) {
    if (amount < 0 || amount > 255) {
      throw new IllegalArgumentException(
          "The " + name + " amount " + amount + " is out of range. It must be from 0 to 255.");
    }
  }
}
