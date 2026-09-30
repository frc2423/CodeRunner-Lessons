package frc.robot.lab;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What your controller can see of the mechanism it controls: the goal, the sensor readings, and the
 * time. It is passed to {@link Controller#calculate} every loop.
 *
 * <p>Units depend on the mechanism:
 *
 * <ul>
 *   <li><b>Flywheel</b>: velocity in RPM (rotations per minute), position in rotations.
 *   <li><b>Swerve steering</b>: position in degrees, from -180 to 180; velocity in degrees per
 *       second.
 *   <li><b>Elevator</b>: position in meters (0 is the bottom); velocity in meters per second.
 *   <li><b>Arm</b>: position in radians (0 is horizontal, pointing forward; up is positive);
 *       velocity in radians per second.
 * </ul>
 *
 * <p>Sensor readings are what a real encoder would report. In some challenges they are noisy, just
 * like on a real robot.
 *
 * <p>You don't need to edit this file.
 */
public final class Mechanism {
  /** The time between loops, in seconds. */
  public static final double DT = 0.02;

  private double goal;
  private double position;
  private double velocity;
  private double time;
  private double appliedVoltage;
  private double estimate = Double.NaN;
  private final Map<String, Double> plots = new LinkedHashMap<>();

  Mechanism() {}

  /**
   * Where the trial wants the mechanism to be right now: a speed for a flywheel, a position for
   * everything else. It changes during a trial.
   */
  public double goal() {
    return goal;
  }

  /** The measured position. */
  public double position() {
    return position;
  }

  /** The measured velocity. */
  public double velocity() {
    return velocity;
  }

  /** Seconds since the trial started: 0.0, 0.02, 0.04, … */
  public double time() {
    return time;
  }

  /** The time between loops, in seconds (0.02). */
  public double dt() {
    return DT;
  }

  /**
   * The voltage the motor actually got last loop: what your controller returned, clamped to the
   * battery's -12 to 12 volts. It is 0 on the first loop.
   */
  public double appliedVoltage() {
    return appliedVoltage;
  }

  /**
   * Draws a line on the dashboard's plot, so you can see what your code is doing. Call it every
   * loop with the same name, for example {@code mechanism.plot("Error", error);}.
   *
   * @param name the line's name in the plot legend
   * @param value the value to draw, in the same units as the goal
   */
  public void plot(String name, double value) {
    plots.put(name, value);
  }

  /**
   * Tells the lab your best guess of the mechanism's true velocity (for a flywheel) or position
   * (for everything else). The dashboard draws it as <b>Your estimate</b>, and the noise and Kalman
   * filter challenges grade how close it is to the truth.
   *
   * @param value your estimate, in the same units as the goal
   */
  public void showEstimate(double value) {
    estimate = value;
  }

  // Used by the lab ------------------------------------------------------------------------------

  void update(double goal, double position, double velocity, double time, double appliedVoltage) {
    this.goal = goal;
    this.position = position;
    this.velocity = velocity;
    this.time = time;
    this.appliedVoltage = appliedVoltage;
    estimate = Double.NaN;
    plots.clear();
  }

  double estimate() {
    return estimate;
  }

  Map<String, Double> plots() {
    return plots;
  }
}
