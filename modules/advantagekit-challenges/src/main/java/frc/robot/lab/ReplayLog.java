package frc.robot.lab;

import edu.wpi.first.util.datalog.DataLogReader;
import edu.wpi.first.util.datalog.DataLogRecord;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Reads the log a replay wrote. It holds both sides of the run: {@code RealOutputs} (what the
 * robot's code did the first time, copied from the original log) and {@code ReplayOutputs} (what
 * the code did this time, with the same inputs). AdvantageScope reads the same file.
 */
final class ReplayLog {
  private static final String REAL = "/RealOutputs/";
  private static final String REPLAY = "/ReplayOutputs/";
  private static final List<String> NOT_COMPARED =
      List.of("Lab/", "LoggedRobot/", "Logger/", "Console", "Alerts/", ".schema/");
  private static final List<String> NOT_INPUTS =
      List.of(
          REAL,
          REPLAY,
          "/.schema",
          "/Timestamp",
          "/SystemStats",
          "/DriverStation",
          "/PowerDistribution",
          "/RadioStatus",
          "/Lab/");
  private static final double TOLERANCE = 1e-6;

  /** One value of one key over the run: time in microseconds to the value set at that time. */
  record Series(String type, NavigableMap<Long, Object> values) {
    /** The value in force at {@code time}, or null before the first one. */
    Object at(long time) {
      Map.Entry<Long, Object> entry = values.floorEntry(time);
      return entry == null ? null : entry.getValue();
    }
  }

  /** The first moment a key's replayed value left its real value. */
  record Difference(String key, double time, String real, String replay, int cycles) {}

  private final Map<String, Series> all = new HashMap<>();
  private final TreeSet<Long> cycles = new TreeSet<>();

  private ReplayLog() {}

  static ReplayLog read(String path) throws IOException {
    ReplayLog log = new ReplayLog();
    DataLogReader reader = new DataLogReader(path);
    if (!reader.isValid()) {
      throw new IOException("not a valid log file: " + path);
    }
    Map<Integer, Series> byId = new HashMap<>();
    Map<Integer, String> names = new HashMap<>();
    for (DataLogRecord record : reader) {
      if (record.isStart()) {
        DataLogRecord.StartRecordData start = record.getStartData();
        Series series = new Series(start.type, new TreeMap<>());
        byId.put(start.entry, series);
        names.put(start.entry, start.name);
        log.all.put(start.name, series);
      } else if (!record.isControl()) {
        Series series = byId.get(record.getEntry());
        if (series == null) {
          continue;
        }
        Object value = decode(record, series.type());
        if (value != null) {
          series.values().put(record.getTimestamp(), value);
        }
        if ("/Timestamp".equals(names.get(record.getEntry()))) {
          log.cycles.add(record.getTimestamp());
        }
      }
    }
    return log;
  }

  private static Object decode(DataLogRecord record, String type) {
    try {
      return switch (type) {
        case "double" -> record.getDouble();
        case "float" -> (double) record.getFloat();
        case "int64" -> (double) record.getInteger();
        case "boolean" -> record.getBoolean();
        case "string" -> record.getString();
        case "double[]" -> record.getDoubleArray();
        case "float[]" -> toDoubles(record.getFloatArray());
        case "int64[]" -> toDoubles(record.getIntegerArray());
        case "boolean[]" -> record.getBooleanArray();
        case "string[]" -> record.getStringArray();
        default -> record.getRaw();
      };
    } catch (RuntimeException e) {
      return null;
    }
  }

  private static double[] toDoubles(float[] values) {
    double[] out = new double[values.length];
    for (int i = 0; i < values.length; i++) {
      out[i] = values[i];
    }
    return out;
  }

  private static double[] toDoubles(long[] values) {
    double[] out = new double[values.length];
    for (int i = 0; i < values.length; i++) {
      out[i] = values[i];
    }
    return out;
  }

  private static boolean compared(String key) {
    return NOT_COMPARED.stream().noneMatch(key::startsWith);
  }

  /** The student-visible keys logged as outputs on the real run, without the table prefix. */
  List<String> realKeys() {
    return keysUnder(REAL);
  }

  /** The student-visible keys logged as outputs on the replay, without the table prefix. */
  List<String> replayKeys() {
    return keysUnder(REPLAY);
  }

  private List<String> keysUnder(String prefix) {
    List<String> keys = new ArrayList<>();
    for (Map.Entry<String, Series> entry : all.entrySet()) {
      String name = entry.getKey();
      if (name.startsWith(prefix) && !entry.getValue().values().isEmpty()) {
        String key = name.substring(prefix.length());
        if (compared(key)) {
          keys.add(key);
        }
      }
    }
    keys.sort(null);
    return keys;
  }

  /** The keys the student's IO layers logged as inputs, without the leading slash. */
  List<String> inputKeys() {
    List<String> keys = new ArrayList<>();
    for (Map.Entry<String, Series> entry : all.entrySet()) {
      String name = entry.getKey();
      boolean theirs =
          NOT_INPUTS.stream().noneMatch(name::startsWith) && !entry.getValue().values().isEmpty();
      Object first = theirs ? entry.getValue().values().firstEntry().getValue() : null;
      if (first instanceof Double || first instanceof Boolean) {
        keys.add(name.substring(1));
      }
    }
    keys.sort(null);
    return keys;
  }

  Series real(String key) {
    return all.get(REAL + key);
  }

  Series replay(String key) {
    return all.get(REPLAY + key);
  }

  /** An input, by its full key (for example {@code /Flywheel/VelocityRadPerSec}). */
  Series input(String key) {
    return all.get(key);
  }

  /** A string the lab wrote during the replay, or "". */
  String labText(String key) {
    Series series = replay("Lab/" + key);
    if (series == null || series.values().isEmpty()) {
      return "";
    }
    return String.valueOf(series.values().lastEntry().getValue());
  }

  /** A number the lab wrote during the replay, or 0. */
  double labNumber(String key) {
    Series series = replay("Lab/" + key);
    if (series == null || series.values().isEmpty()) {
      return 0;
    }
    return series.values().lastEntry().getValue() instanceof Number number
        ? number.doubleValue()
        : 0;
  }

  long startTime() {
    return cycles.isEmpty() ? 0 : cycles.first();
  }

  double seconds(long time) {
    return (time - startTime()) / 1e6;
  }

  double duration() {
    return cycles.isEmpty() ? 0 : seconds(cycles.last());
  }

  int cycleCount() {
    return cycles.size();
  }

  /** How each key that both runs logged compares, worst first. */
  List<Difference> differences() {
    List<Difference> out = new ArrayList<>();
    for (String key : realKeys()) {
      Series real = real(key);
      Series replay = replay(key);
      if (replay == null) {
        continue;
      }
      Difference found = compare(key, real, replay);
      if (found != null) {
        out.add(found);
      }
    }
    out.sort((a, b) -> Double.compare(a.time(), b.time()));
    return out;
  }

  private Difference compare(String key, Series real, Series replay) {
    long first = -1;
    int count = 0;
    for (long time : cycles) {
      if (!same(real.at(time), replay.at(time))) {
        if (first < 0) {
          first = time;
        }
        count++;
      }
    }
    if (first < 0) {
      return null;
    }
    return new Difference(
        key, seconds(first), describe(real.at(first)), describe(replay.at(first)), count);
  }

  static boolean same(Object a, Object b) {
    if (a == null || b == null) {
      return a == b;
    }
    if (a instanceof Double x && b instanceof Double y) {
      return Math.abs(x - y) <= TOLERANCE;
    }
    if (a instanceof double[] x && b instanceof double[] y) {
      if (x.length != y.length) {
        return false;
      }
      for (int i = 0; i < x.length; i++) {
        if (Math.abs(x[i] - y[i]) > TOLERANCE) {
          return false;
        }
      }
      return true;
    }
    if (a instanceof boolean[] x && b instanceof boolean[] y) {
      return Arrays.equals(x, y);
    }
    if (a instanceof String[] x && b instanceof String[] y) {
      return Arrays.equals(x, y);
    }
    if (a instanceof byte[] x && b instanceof byte[] y) {
      return Arrays.equals(x, y);
    }
    return a.equals(b);
  }

  static String describe(Object value) {
    if (value == null) {
      return "nothing";
    }
    if (value instanceof Double number) {
      return Check.f("%.4g", number);
    }
    if (value instanceof double[] array) {
      return "an array of " + array.length;
    }
    if (value instanceof byte[]) {
      return "a different value";
    }
    return String.valueOf(value);
  }
}
