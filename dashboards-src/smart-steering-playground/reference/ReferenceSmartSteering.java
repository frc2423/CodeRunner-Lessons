package frc.robot.playground;

import static frc.robot.playground.PlaygroundConstants.*;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rectangle2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import java.util.List;

/**
 * A reference solution for SmartSteering.steer, kept out of the module so students don't see it.
 *
 * <ol>
 *   <li>Slide: for every nearby obstacle, cap the part of the speed heading into it at the fastest
 *       speed the robot can still stop from, and leave the part along it alone.
 *   <li>Brake first: if the robot is <i>already</i> moving toward something faster than that, spend
 *       the whole acceleration budget on braking (the wheels can only change the velocity so much
 *       per loop, in any direction combined).
 *   <li>Don't spin a corner into something.
 * </ol>
 */
public class ReferenceSmartSteering {
  /** Plan to brake gentler than the robot can: in a corner, it has to brake in two directions at once. */
  static final double BRAKE = 0.6 * MAX_ACCELERATION;

  /** Keep this much space between the bumpers and any obstacle. */
  static final double MARGIN = 0.04;

  /** Ignore obstacles farther than this. */
  static final double LOOK = 3.0;

  public ChassisSpeeds steer(
      ChassisSpeeds requested,
      Pose2d robotPose,
      ChassisSpeeds currentSpeeds,
      List<Rectangle2d> obstacles) {
    Rectangle2d robot = Geometry.robotFootprint(robotPose);
    double vx = requested.vxMetersPerSecond;
    double vy = requested.vyMetersPerSecond;
    double cx = currentSpeeds.vxMetersPerSecond;
    double cy = currentSpeeds.vyMetersPerSecond;

    Translation2d brakeAxis = null;
    double worstExcess = 0;
    for (int pass = 0; pass < 3; pass++) {
      for (Rectangle2d obstacle : obstacles) {
        double gap = Geometry.distance(robot, obstacle);
        if (gap > LOOK) {
          continue;
        }
        Translation2d away = awayFrom(robot, obstacle, gap);
        if (away == null) {
          continue;
        }
        double allowed = allowedApproach(gap);
        double into = -(vx * away.getX() + vy * away.getY());
        if (into > allowed) {
          vx += (into - allowed) * away.getX();
          vy += (into - allowed) * away.getY();
        }
        double intoNow = -(cx * away.getX() + cy * away.getY());
        if (pass == 0 && intoNow - allowed > worstExcess) {
          worstExcess = intoNow - allowed;
          brakeAxis = away;
        }
      }
    }

    if (brakeAxis != null) {
      // Braking matters more than anything else the driver asked for this loop.
      double budget = MAX_ACCELERATION * LOOP_PERIOD;
      double dx = vx - cx;
      double dy = vy - cy;
      double along = dx * brakeAxis.getX() + dy * brakeAxis.getY();
      double sideX = dx - along * brakeAxis.getX();
      double sideY = dy - along * brakeAxis.getY();
      along = Math.max(-budget, Math.min(budget, along));
      double sideBudget = Math.sqrt(Math.max(0, budget * budget - along * along));
      double side = Math.hypot(sideX, sideY);
      if (side > sideBudget) {
        sideX *= sideBudget / side;
        sideY *= sideBudget / side;
      }
      vx = cx + along * brakeAxis.getX() + sideX;
      vy = cy + along * brakeAxis.getY() + sideY;
    }

    return new ChassisSpeeds(vx, vy, safeSpin(robotPose, vx, vy, requested, currentSpeeds, obstacles));
  }

  /** The fastest the robot may approach an obstacle {@code gap} meters away and still stop. */
  private static double allowedApproach(double gap) {
    // The robot reacts one loop late, so take that loop's travel off the room it has.
    double room = Math.max(0, gap - MARGIN);
    return Math.max(0, Math.sqrt(2 * BRAKE * room) - BRAKE * LOOP_PERIOD);
  }

  /** The requested spin, or none if spinning would swing a corner into something. */
  private static double safeSpin(
      Pose2d pose,
      double vx,
      double vy,
      ChassisSpeeds requested,
      ChassisSpeeds current,
      List<Rectangle2d> obstacles) {
    double omega = requested.omegaRadiansPerSecond;
    // How far the robot will turn before it could stop spinning, plus a little.
    double spinNow = current.omegaRadiansPerSecond;
    double stopAngle = spinNow * Math.abs(spinNow) / (2 * MAX_ANGULAR_ACCELERATION);
    double turn = stopAngle + omega * 0.1;
    double ahead = 0.1;
    Pose2d slid = new Pose2d(pose.getX() + vx * ahead, pose.getY() + vy * ahead, pose.getRotation());
    Pose2d spun =
        new Pose2d(slid.getTranslation(), pose.getRotation().plus(Rotation2d.fromRadians(turn)));
    Rectangle2d slidFootprint = Geometry.robotFootprint(slid);
    Rectangle2d spunFootprint = Geometry.robotFootprint(spun);
    for (Rectangle2d obstacle : obstacles) {
      double spunGap = Geometry.distance(spunFootprint, obstacle);
      if (spunGap < MARGIN && spunGap <= Geometry.distance(slidFootprint, obstacle)) {
        return 0;
      }
    }
    return omega;
  }

  /** The direction from the obstacle toward the robot, at their closest points. */
  private static Translation2d awayFrom(Rectangle2d robot, Rectangle2d obstacle, double gap) {
    if (gap < 1e-3) {
      // Touching: grow the robot a little and see which way it would be pushed out.
      Rectangle2d bigger =
          new Rectangle2d(robot.getCenter(), robot.getXWidth() + 0.02, robot.getYWidth() + 0.02);
      Translation2d push = Geometry.separation(bigger, obstacle);
      return push.getNorm() < 1e-9 ? null : push.div(push.getNorm());
    }
    Translation2d best = null;
    double bestDistance = Double.POSITIVE_INFINITY;
    for (Translation2d corner : Geometry.corners(robot)) {
      Translation2d near = obstacle.nearest(corner);
      double d = corner.getDistance(near);
      if (d < bestDistance) {
        bestDistance = d;
        best = corner.minus(near);
      }
    }
    for (Translation2d corner : Geometry.corners(obstacle)) {
      Translation2d near = robot.nearest(corner);
      double d = corner.getDistance(near);
      if (d < bestDistance) {
        bestDistance = d;
        best = near.minus(corner);
      }
    }
    return best == null || best.getNorm() < 1e-9 ? null : best.div(best.getNorm());
  }
}
