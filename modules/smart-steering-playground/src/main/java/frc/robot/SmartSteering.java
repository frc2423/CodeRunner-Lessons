package frc.robot;

import static frc.robot.playground.PlaygroundConstants.*;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rectangle2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import frc.robot.playground.Debug;
import frc.robot.playground.Geometry;
import java.util.List;

/**
 * Smart steering, like in Mario Kart: the driver still drives, but the robot quietly helps so it
 * doesn't crash into things.
 *
 * <p>Every loop (every 20 ms), the playground reads the driver's sticks, turns them into the speeds
 * the driver is asking for, and calls {@link #steer}. Whatever {@code steer} returns is what the
 * robot actually drives at. Right now it returns exactly what the driver asked for, so the robot
 * crashes into everything. Your job is to make it smarter.
 *
 * <p>The obstacles aren't solid: if your code lets the robot drive into one, it drives right
 * through it, the obstacle lights up red, and it counts as a bump. That way you can always see when
 * your steering didn't work. The field walls are solid, though.
 *
 * <p>Good smart steering:
 *
 * <ul>
 *   <li>never lets the robot hit an obstacle or a field wall, however the driver drives;
 *   <li>changes as little as possible: away from obstacles, the robot does exactly what the driver
 *       asks;
 *   <li>helps instead of stopping: near a wall, the robot slides along it rather than getting
 *       stuck;
 *   <li>never stops the driver from driving <i>away</i> from an obstacle.
 * </ul>
 *
 * <p>Everything is <b>field-relative</b>: {@code vxMetersPerSecond} is along the field's X (to the
 * right on the Playground tab), {@code vyMetersPerSecond} is along the field's Y (up on the tab),
 * and {@code omegaRadiansPerSecond} is the spin, counterclockwise positive. Useful numbers like
 * {@link frc.robot.playground.PlaygroundConstants#MAX_ACCELERATION MAX_ACCELERATION} and {@link
 * frc.robot.playground.PlaygroundConstants#ROBOT_LENGTH ROBOT_LENGTH} are in {@code
 * PlaygroundConstants}.
 *
 * <p>Save, then click <b>Restart</b> in the Driver Station to try your changes. The <b>Smart
 * steering</b> switch on the Playground tab turns your code on and off, so you can compare.
 */
public class SmartSteering {
  // Anything you want to remember from one loop to the next can go in fields here.

  /**
   * Decides what the robot should really do, given what the driver asked for.
   *
   * @param requested the speeds the driver is asking for, field-relative
   * @param robotPose where the robot is now, and which way it faces
   * @param currentSpeeds how fast the robot is moving now, field-relative. It can't change speed
   *     instantly: it takes {@code speed * speed / (2 * MAX_ACCELERATION)} meters to stop.
   * @param obstacles everything the robot should stay out of: the obstacles from {@link Obstacles},
   *     the ones drawn on the Playground tab, and the four field walls (as rectangles just outside
   *     the field)
   * @return the speeds the robot should drive at, field-relative
   */
  public ChassisSpeeds steer(
      ChassisSpeeds requested,
      Pose2d robotPose,
      ChassisSpeeds currentSpeeds,
      List<Rectangle2d> obstacles) {
    // The robot's bumpers, as a rectangle you can test against the obstacles.
    Rectangle2d robot = Geometry.robotFootprint(robotPose);

    // Show something on the Playground tab. Debug values and drawings are a big help!
    Debug.value(
        "Requested speed (m/s)",
        Math.hypot(requested.vxMetersPerSecond, requested.vyMetersPerSecond));
    Debug.value("Closest obstacle (m)", closestDistance(robot, obstacles));

    // TODO: Make the robot steer smartly. Some ideas, from simplest to smartest:
    //
    //  1. Look ahead. Where will the robot be in a moment if it drives at the requested speed?
    //     (Hint: robotPose.getX() + requested.vxMetersPerSecond * time, and the same for Y.)
    //     Make a footprint there with Geometry.robotFootprint, draw it with Debug.rectangle,
    //     and check it against every obstacle with Geometry.overlaps.
    //
    //  2. Brake. If the robot would hit something, slow it down (or stop it) before it does.
    //
    //  3. Slide. Instead of stopping, remove only the part of the speed that points INTO the
    //     obstacle, and keep the part that slides along it. Geometry.separation tells you which
    //     way an obstacle's surface faces.
    //
    //  4. Be smooth. The closer the robot is to an obstacle, the slower it may approach it.
    //     From distance d, the fastest speed it can still stop from is
    //     Math.sqrt(2 * MAX_ACCELERATION * d).
    //
    //  5. Steer. Like in Mario Kart, gently turn the robot around obstacles in its way.

    return requested;
  }

  /** How far the robot is from the closest obstacle (or wall). */
  private static double closestDistance(Rectangle2d robot, List<Rectangle2d> obstacles) {
    double closest = Double.POSITIVE_INFINITY;
    for (Rectangle2d obstacle : obstacles) {
      closest = Math.min(closest, Geometry.distance(robot, obstacle));
    }
    return closest;
  }
}
