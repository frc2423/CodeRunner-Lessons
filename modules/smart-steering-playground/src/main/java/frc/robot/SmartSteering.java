package frc.robot;

import static frc.robot.playground.PlaygroundConstants.*;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rectangle2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import frc.robot.playground.Debug;
import java.util.List;

public class SmartSteering {

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
    // Show something on the Playground tab. Debug values and drawings are a big help!
    Debug.value(
        "Requested speed (m/s)",
        Math.hypot(requested.vxMetersPerSecond, requested.vyMetersPerSecond));

    return requested;
  }
}
