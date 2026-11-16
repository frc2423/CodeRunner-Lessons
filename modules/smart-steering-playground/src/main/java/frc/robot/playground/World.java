package frc.robot.playground;

import static frc.robot.playground.PlaygroundConstants.*;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rectangle2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import java.util.ArrayList;
import java.util.List;

/**
 * The simulated swerve robot. Each loop it accelerates toward the field-relative speeds it's given
 * (no faster than {@link PlaygroundConstants#MAX_ACCELERATION}) and moves. Nothing is solid: the
 * robot drives straight through obstacles, so it's easy to see when smart steering didn't keep it
 * out. The only limit is that the robot can't leave the field.
 *
 * <p>You don't need to edit this file.
 */
final class World {
  private double time = 0;
  private double x;
  private double y;
  private double heading;
  private double vx = 0;
  private double vy = 0;
  private double omega = 0;

  private double distance = 0;
  private int resets = 0;

  World() {
    reset(START_POSE);
    resets = 0;
  }

  /** Puts the robot back at {@code pose}, stopped. */
  void reset(Pose2d pose) {
    x = pose.getX();
    y = pose.getY();
    heading = pose.getRotation().getRadians();
    vx = 0;
    vy = 0;
    omega = 0;
    distance = 0;
    resets++;
  }

  /** Runs one loop: accelerate toward {@code target} (field-relative) and move. */
  void step(ChassisSpeeds target) {
    double dt = LOOP_PERIOD;
    time += dt;

    // Accelerate toward the target, no faster than the wheels can grip.
    double targetVx = target.vxMetersPerSecond;
    double targetVy = target.vyMetersPerSecond;
    double targetSpeed = Math.hypot(targetVx, targetVy);
    if (targetSpeed > MAX_SPEED) {
      targetVx *= MAX_SPEED / targetSpeed;
      targetVy *= MAX_SPEED / targetSpeed;
    }
    double dvx = targetVx - vx;
    double dvy = targetVy - vy;
    double dv = Math.hypot(dvx, dvy);
    double maxDv = MAX_ACCELERATION * dt;
    if (dv > maxDv) {
      dvx *= maxDv / dv;
      dvy *= maxDv / dv;
    }
    vx += dvx;
    vy += dvy;
    double targetOmega =
        Math.max(-MAX_ANGULAR_SPEED, Math.min(MAX_ANGULAR_SPEED, target.omegaRadiansPerSecond));
    double maxDOmega = MAX_ANGULAR_ACCELERATION * dt;
    omega += Math.max(-maxDOmega, Math.min(maxDOmega, targetOmega - omega));

    double startX = x;
    double startY = y;
    x += vx * dt;
    y += vy * dt;
    heading = Rotation2d.fromRadians(heading + omega * dt).getRadians();

    // Keep the robot on the field (roughly: its center stays half a robot away from the edges).
    double margin = ROBOT_WIDTH / 2;
    if (x < margin || x > FIELD_LENGTH - margin) {
      x = Math.max(margin, Math.min(FIELD_LENGTH - margin, x));
      vx = 0;
    }
    if (y < margin || y > FIELD_WIDTH - margin) {
      y = Math.max(margin, Math.min(FIELD_WIDTH - margin, y));
      vy = 0;
    }
    distance += Math.hypot(x - startX, y - startY);
  }

  /** The four field walls, as rectangles just outside the field. */
  static List<Rectangle2d> walls() {
    double t = WALL_THICKNESS;
    List<Rectangle2d> walls = new ArrayList<>();
    walls.add(rect(FIELD_LENGTH / 2, -t / 2, FIELD_LENGTH + 2 * t, t));
    walls.add(rect(FIELD_LENGTH / 2, FIELD_WIDTH + t / 2, FIELD_LENGTH + 2 * t, t));
    walls.add(rect(-t / 2, FIELD_WIDTH / 2, t, FIELD_WIDTH + 2 * t));
    walls.add(rect(FIELD_LENGTH + t / 2, FIELD_WIDTH / 2, t, FIELD_WIDTH + 2 * t));
    return walls;
  }

  private static Rectangle2d rect(double cx, double cy, double xWidth, double yWidth) {
    return new Rectangle2d(new Pose2d(cx, cy, Rotation2d.kZero), xWidth, yWidth);
  }

  Pose2d pose() {
    return new Pose2d(x, y, Rotation2d.fromRadians(heading));
  }

  /** The robot's speeds right now, field-relative. */
  ChassisSpeeds fieldSpeeds() {
    return new ChassisSpeeds(vx, vy, omega);
  }

  double time() {
    return time;
  }

  double distance() {
    return distance;
  }

  int resets() {
    return resets;
  }
}
