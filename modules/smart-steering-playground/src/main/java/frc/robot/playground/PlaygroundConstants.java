package frc.robot.playground;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;

/**
 * Numbers that describe the playground: the field, the robot, and how fast it can go. Use them in
 * {@code SmartSteering.java}. All distances are in meters, angles in radians, and times in seconds.
 *
 * <p>Like on a real FRC field, the origin is the corner to the right of the blue driver station: +X
 * points away from the blue driver station (to the right on the Playground tab) and +Y points to
 * the left, as the blue driver sees it (up on the tab). Counterclockwise rotation is positive.
 */
public final class PlaygroundConstants {
  private PlaygroundConstants() {}

  /** The field's length, along X. */
  public static final double FIELD_LENGTH = 16.541;

  /** The field's width, along Y. */
  public static final double FIELD_WIDTH = 8.069;

  /** The robot's length (bumper to bumper), front to back. Its front is where the arrow points. */
  public static final double ROBOT_LENGTH = 0.9;

  /** The robot's width (bumper to bumper), side to side. */
  public static final double ROBOT_WIDTH = 0.75;

  /** The fastest the robot can drive, in meters per second. */
  public static final double MAX_SPEED = 4.5;

  /** The fastest the robot can spin, in radians per second. */
  public static final double MAX_ANGULAR_SPEED = 1.5 * Math.PI;

  /**
   * How quickly the robot can change its speed, in meters per second per second. The wheels can't
   * grip any harder than this, so the robot takes {@code speed * speed / (2 * MAX_ACCELERATION)}
   * meters to stop.
   */
  public static final double MAX_ACCELERATION = 8.0;

  /** How quickly the robot can change its spin, in radians per second per second. */
  public static final double MAX_ANGULAR_ACCELERATION = 6 * Math.PI;

  /** How often {@code SmartSteering.steer} is called: every 20 ms. */
  public static final double LOOP_PERIOD = 0.02;

  /** How thick the field walls are in the obstacle list. Only their inside faces matter. */
  public static final double WALL_THICKNESS = 1.0;

  /** Where the robot starts, and where it goes back to when you click Reset. */
  public static final Pose2d START_POSE = new Pose2d(1.5, FIELD_WIDTH / 2, Rotation2d.kZero);
}
