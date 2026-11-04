package frc.robot.lab;

import edu.wpi.first.math.geometry.Pose2d;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.littletonrobotics.junction.AutoLogOutputManager;
import org.littletonrobotics.junction.LogTable;
import org.littletonrobotics.junction.LogTable.LogValue;
import org.littletonrobotics.junction.Logger;

/**
 * Lets the lab look inside AdvantageKit's log table, so it can show and grade what your code logged
 * this loop. AdvantageKit doesn't expect a robot program to change challenges while it runs, so
 * this class also clears what the last challenge left behind.
 *
 * <p>Real robot code never needs to do any of this. You don't need to edit this file.
 */
final class LogAccess {
  /** Tables AdvantageKit fills by itself. They'd only hide what the student logged. */
  private static final List<String> BUILT_IN =
      List.of(
          "/.schema",
          "/SystemStats",
          "/DriverStation",
          "/PowerDistribution",
          "/RadioStatus",
          "/Lab/",
          "/Timestamp",
          "/RealOutputs/Lab/",
          "/RealOutputs/Logger/",
          "/RealOutputs/LoggedRobot/",
          "/RealOutputs/Console",
          "/RealOutputs/Alerts/",
          "/ReplayOutputs/");

  private static final String OUTPUTS = "/RealOutputs/";
  private static final String NETWORK = "/NetworkInputs/";

  private LogAccess() {}

  private static LogTable entry() {
    try {
      Field field = Logger.class.getDeclaredField("entry");
      field.setAccessible(true);
      return (LogTable) field.get(null);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Can't read AdvantageKit's log table", e);
    }
  }

  private static boolean builtIn(String key) {
    for (String prefix : BUILT_IN) {
      if (key.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  /** Every value the student's code has logged, by full key. */
  static Map<String, LogValue> snapshot() {
    Map<String, LogValue> out = new HashMap<>();
    for (Map.Entry<String, LogValue> entry : entry().getAll(false).entrySet()) {
      if (!builtIn(entry.getKey())) {
        out.put(entry.getKey(), entry.getValue());
      }
    }
    return out;
  }

  /** Removes every value the student's code logged, so a new run starts with a clean log. */
  static void clear() {
    removeStudentValues(entry());
  }

  /** Removes every value the student's code logged from this table. */
  static void removeStudentValues(LogTable table) {
    table.getAll(false).keySet().removeIf(key -> !builtIn(key));
  }

  /** Forgets every dashboard input made so far, so an old challenge's stop being logged. */
  static void resetDashboardInputs() {
    try {
      Field field = Logger.class.getDeclaredField("dashboardInputs");
      field.setAccessible(true);
      ((Collection<?>) field.get(null)).clear();
    } catch (ReflectiveOperationException | RuntimeException e) {
      System.out.println("[AKitLab] Couldn't reset the dashboard inputs: " + e);
    }
  }

  /** Forgets every {@code @AutoLogOutput} found so far, so an old challenge stops logging. */
  static void resetAutoLog() {
    try {
      for (String name : List.of("callbacks", "scannedObjectHashes")) {
        Field field = AutoLogOutputManager.class.getDeclaredField(name);
        field.setAccessible(true);
        ((Collection<?>) field.get(null)).clear();
      }
    } catch (ReflectiveOperationException | RuntimeException e) {
      System.out.println("[AKitLab] Couldn't reset @AutoLogOutput: " + e);
    }
  }

  /** "output", "dashboard" or "input". */
  static String kind(String key) {
    if (key.startsWith(OUTPUTS)) {
      return "output";
    }
    return key.startsWith(NETWORK) ? "dashboard" : "input";
  }

  /** The key as it reads in AdvantageScope's tables, without the leading slash or RealOutputs. */
  static String shortKey(String key) {
    if (key.startsWith(OUTPUTS)) {
      return key.substring(OUTPUTS.length());
    }
    return key.startsWith("/") ? key.substring(1) : key;
  }

  static String typeName(LogValue value) {
    String custom = value.customTypeStr;
    if (custom != null && !custom.isEmpty()) {
      return custom;
    }
    return value.getWPILOGType();
  }

  static Double number(LogValue value) {
    if (value == null) {
      return null;
    }
    return switch (value.type) {
      case Double -> value.getDouble();
      case Float -> (double) value.getFloat();
      case Integer -> (double) value.getInteger();
      case Boolean -> value.getBoolean() ? 1.0 : 0.0;
      default -> null;
    };
  }

  static Pose2d pose(LogValue value) {
    if (value == null || !"struct:Pose2d".equals(value.customTypeStr)) {
      return null;
    }
    return Pose2d.struct.unpack(ByteBuffer.wrap(value.getRaw()).order(ByteOrder.LITTLE_ENDIAN));
  }

  static Pose2d[] poses(LogValue value) {
    if (value == null || !"struct:Pose2d[]".equals(value.customTypeStr)) {
      return null;
    }
    ByteBuffer buffer = ByteBuffer.wrap(value.getRaw()).order(ByteOrder.LITTLE_ENDIAN);
    Pose2d[] out = new Pose2d[buffer.remaining() / Pose2d.struct.getSize()];
    for (int i = 0; i < out.length; i++) {
      out[i] = Pose2d.struct.unpack(buffer);
    }
    return out;
  }

  /** Writes a logged value as JSON, for the dashboard. */
  static void write(Json json, LogValue value) {
    switch (value.type) {
      case Double -> json.value(value.getDouble());
      case Float -> json.value(value.getFloat());
      case Integer -> json.value(value.getInteger());
      case Boolean -> json.value(value.getBoolean());
      case String -> json.value(value.getString());
      case DoubleArray -> writeArray(json, value.getDoubleArray());
      case FloatArray -> {
        float[] floats = value.getFloatArray();
        double[] doubles = new double[floats.length];
        for (int i = 0; i < floats.length; i++) {
          doubles[i] = floats[i];
        }
        writeArray(json, doubles);
      }
      case IntegerArray -> {
        long[] longs = value.getIntegerArray();
        double[] doubles = new double[longs.length];
        for (int i = 0; i < longs.length; i++) {
          doubles[i] = longs[i];
        }
        writeArray(json, doubles);
      }
      case BooleanArray -> {
        json.beginArray();
        for (boolean item : value.getBooleanArray()) {
          json.value(item);
        }
        json.endArray();
      }
      case StringArray -> {
        json.beginArray();
        for (String item : value.getStringArray()) {
          json.value(item);
        }
        json.endArray();
      }
      default -> {
        Pose2d pose = pose(value);
        Pose2d[] poses = pose == null ? poses(value) : null;
        if (pose != null) {
          writePose(json, pose);
        } else if (poses != null) {
          json.beginArray();
          for (Pose2d item : poses) {
            writePose(json, item);
          }
          json.endArray();
        } else {
          json.value(typeName(value));
        }
      }
    }
  }

  private static void writeArray(Json json, double[] values) {
    json.beginArray();
    for (double value : values) {
      json.value(value);
    }
    json.endArray();
  }

  private static void writePose(Json json, Pose2d pose) {
    json.beginObject()
        .key("x")
        .value(pose.getX())
        .key("y")
        .value(pose.getY())
        .key("r")
        .value(pose.getRotation().getRadians())
        .endObject();
  }
}
