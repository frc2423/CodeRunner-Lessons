package frc.robot.playground;

import edu.wpi.first.math.geometry.Rectangle2d;
import edu.wpi.first.math.geometry.Translation2d;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Draw on the Playground tab's field and show numbers next to it, to see what your steering code is
 * thinking. Call these from {@code SmartSteering.steer}: everything you draw shows for one loop, so
 * draw it again every loop to keep it on screen.
 *
 * <pre>{@code
 * Debug.line(robotPose.getTranslation(), lookAheadPoint, Debug.Color.YELLOW);
 * Debug.rectangle(futureFootprint, Debug.Color.RED);
 * Debug.value("Closest obstacle (m)", closest);
 * }</pre>
 */
public final class Debug {
  private Debug() {}

  /** The colors you can draw with. */
  public enum Color {
    YELLOW,
    RED,
    GREEN,
    BLUE,
    PURPLE,
    WHITE
  }

  /** At most this many shapes are drawn each loop; the rest are dropped. */
  public static final int MAX_SHAPES = 400;

  static final int POINT = 0;
  static final int LINE = 1;
  static final int RECTANGLE = 2;
  static final int CIRCLE = 3;
  static final int ARROW = 4;

  /** Seven numbers per shape: type, color, then up to five numbers that place it. */
  static final int STRIDE = 7;

  private static final List<double[]> shapes = new ArrayList<>();
  private static final Map<String, String> values = new LinkedHashMap<>();
  private static int dropped = 0;

  /** A dot at {@code point}. */
  public static void point(Translation2d point) {
    point(point, Color.YELLOW);
  }

  /** A dot at {@code point}. */
  public static void point(Translation2d point, Color color) {
    if (point == null) {
      return;
    }
    add(POINT, color, point.getX(), point.getY(), 0, 0, 0);
  }

  /** A line from {@code from} to {@code to}. */
  public static void line(Translation2d from, Translation2d to) {
    line(from, to, Color.YELLOW);
  }

  /** A line from {@code from} to {@code to}. */
  public static void line(Translation2d from, Translation2d to, Color color) {
    if (from == null || to == null) {
      return;
    }
    add(LINE, color, from.getX(), from.getY(), to.getX(), to.getY(), 0);
  }

  /** An arrow from {@code from} to {@code to}. Handy for showing a direction or a speed. */
  public static void arrow(Translation2d from, Translation2d to) {
    arrow(from, to, Color.YELLOW);
  }

  /** An arrow from {@code from} to {@code to}. Handy for showing a direction or a speed. */
  public static void arrow(Translation2d from, Translation2d to, Color color) {
    if (from == null || to == null) {
      return;
    }
    add(ARROW, color, from.getX(), from.getY(), to.getX(), to.getY(), 0);
  }

  /** The outline of a rectangle, for example where the robot will be in a moment. */
  public static void rectangle(Rectangle2d rectangle) {
    rectangle(rectangle, Color.YELLOW);
  }

  /** The outline of a rectangle, for example where the robot will be in a moment. */
  public static void rectangle(Rectangle2d rectangle, Color color) {
    if (rectangle == null) {
      return;
    }
    add(
        RECTANGLE,
        color,
        rectangle.getCenter().getX(),
        rectangle.getCenter().getY(),
        rectangle.getXWidth(),
        rectangle.getYWidth(),
        rectangle.getRotation().getRadians());
  }

  /** The outline of a circle. */
  public static void circle(Translation2d center, double radius) {
    circle(center, radius, Color.YELLOW);
  }

  /** The outline of a circle. */
  public static void circle(Translation2d center, double radius, Color color) {
    if (center == null) {
      return;
    }
    add(CIRCLE, color, center.getX(), center.getY(), radius, 0, 0);
  }

  /** Shows a number in the tab's Debug values list, under {@code name}. */
  public static void value(String name, double value) {
    values.put(
        String.valueOf(name), Double.isFinite(value) ? format(value) : String.valueOf(value));
  }

  /** Shows a true/false value in the tab's Debug values list, under {@code name}. */
  public static void value(String name, boolean value) {
    values.put(String.valueOf(name), String.valueOf(value));
  }

  /** Shows any text (or object) in the tab's Debug values list, under {@code name}. */
  public static void value(String name, Object value) {
    values.put(String.valueOf(name), String.valueOf(value));
  }

  // Used by the playground -----------------------------------------------------------------------

  private static void add(int type, Color color, double a, double b, double c, double d, double e) {
    if (shapes.size() >= MAX_SHAPES) {
      dropped++;
      return;
    }
    double[] numbers = {type, (color == null ? Color.YELLOW : color).ordinal(), a, b, c, d, e};
    for (double number : numbers) {
      if (!Double.isFinite(number)) {
        return;
      }
    }
    shapes.add(numbers);
  }

  private static String format(double value) {
    if (value == Math.rint(value) && Math.abs(value) < 1e9) {
      return String.valueOf((long) value);
    }
    return String.format("%.3f", value);
  }

  /** Forgets last loop's drawings and values. */
  static void clear() {
    shapes.clear();
    values.clear();
    dropped = 0;
  }

  /** Every shape drawn this loop, {@link #STRIDE} numbers each. */
  static double[] shapes() {
    double[] flat = new double[shapes.size() * STRIDE];
    for (int i = 0; i < shapes.size(); i++) {
      System.arraycopy(shapes.get(i), 0, flat, i * STRIDE, STRIDE);
    }
    return flat;
  }

  /** Every value shown this loop, as "name\tvalue". */
  static String[] values() {
    List<String> lines = new ArrayList<>();
    for (Map.Entry<String, String> entry : values.entrySet()) {
      lines.add(entry.getKey().replace('\t', ' ') + "\t" + entry.getValue().replace('\t', ' '));
    }
    if (dropped > 0) {
      lines.add("(too many shapes)\t" + dropped + " not drawn");
    }
    return lines.toArray(new String[0]);
  }
}
