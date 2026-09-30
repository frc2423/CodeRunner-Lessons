package frc.robot.lab;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Everything that happened during a trial, one sample per robot loop, for grading and plots. */
final class Trace {
  /** A growable array of doubles. */
  static final class Series {
    private double[] values = new double[256];
    private int size = 0;

    void add(double value) {
      if (size == values.length) {
        values = Arrays.copyOf(values, size * 2);
      }
      values[size++] = value;
    }

    double get(int i) {
      return values[i];
    }

    int size() {
      return size;
    }

    double[] toArray() {
      return Arrays.copyOf(values, size);
    }
  }

  final Series time = new Series();
  final Series goal = new Series();

  /** The true output (speed for a flywheel, position otherwise), with steering wrapped. */
  final Series output = new Series();

  /** What the sensor reported for the output. */
  final Series measured = new Series();

  /** Distance from the goal, the short way around for steering. */
  final Series error = new Series();

  /** True velocity and position. Steering position here keeps counting past ±180. */
  final Series velocity = new Series();

  final Series position = new Series();
  final Series volts = new Series();
  final Series estimate = new Series();

  /** Lines the controller drew with {@link Mechanism#plot}, padded with NaN where missing. */
  final Map<String, Series> plots = new LinkedHashMap<>();

  int size() {
    return time.size();
  }

  void addPlots(Map<String, Double> values) {
    for (Map.Entry<String, Double> entry : values.entrySet()) {
      Series series = plots.get(entry.getKey());
      if (series == null) {
        if (plots.size() >= 6) {
          continue;
        }
        series = new Series();
        for (int i = 0; i < size() - 1; i++) {
          series.add(Double.NaN);
        }
        plots.put(entry.getKey(), series);
      }
      series.add(entry.getValue());
    }
    for (Series series : plots.values()) {
      while (series.size() < size()) {
        series.add(Double.NaN);
      }
    }
  }

  /** Index of the first sample at or after {@code t}. */
  int indexAt(double t) {
    for (int i = 0; i < size(); i++) {
      if (time.get(i) >= t - 1e-9) {
        return i;
      }
    }
    return size();
  }
}
