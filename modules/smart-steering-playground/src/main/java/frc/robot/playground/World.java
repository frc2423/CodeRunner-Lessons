package frc.robot.playground;

import static frc.robot.playground.PlaygroundConstants.*;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rectangle2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The simulated swerve robot. Each loop it accelerates toward the field-relative speeds it's given
 * (no faster than {@link PlaygroundConstants#MAX_ACCELERATION}), moves, and then bumps into
 * anything in its way: it's pushed back out, and loses the part of its speed that was heading into
 * the obstacle, so it slides along walls instead of going through them.
 *
 * <p>You don't need to edit this file.
 */
final class World {
  /** Hitting something faster than this counts as a bump. */
  static final double BUMP_SPEED = 0.25;

  /** How many recent bumps the dashboard gets, to flash where they happened. */
  static final int RECENT_BUMPS = 12;

  record Bump(double x, double y, double time, double speed) {}

  private double time = 0;
  private double x;
  private double y;
  private double heading;
  private double vx = 0;
  private double vy = 0;
  private double omega = 0;

  private int bumps = 0;
  private double hardestBump = 0;
  private double contactTime = 0;
  private double distance = 0;
  private int resets = 0;
  private boolean touching = false;
  private Set<Rectangle2d> touchingLastLoop = new HashSet<>();
  private final Deque<Bump> recentBumps = new ArrayDeque<>();

  World() {
    reset(START_POSE);
    resets = 0;
  }

  /** Puts the robot back at {@code pose}, stopped, and clears the bump count. */
  void reset(Pose2d pose) {
    x = pose.getX();
    y = pose.getY();
    heading = pose.getRotation().getRadians();
    vx = 0;
    vy = 0;
    omega = 0;
    bumps = 0;
    hardestBump = 0;
    contactTime = 0;
    distance = 0;
    touching = false;
    touchingLastLoop = new HashSet<>();
    recentBumps.clear();
    resets++;
  }

  /** Runs one loop: accelerate toward {@code target} (field-relative), move, and bump. */
  void step(ChassisSpeeds target, List<Rectangle2d> obstacles) {
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

    // Bump: push the robot out of whatever it overlaps, a few times in case pushing it out of one
    // obstacle pushed it into another.
    Set<Rectangle2d> touchingNow = new HashSet<>();
    for (int pass = 0; pass < 4; pass++) {
      boolean moved = false;
      for (Rectangle2d obstacle : obstacles) {
        Translation2d push = Geometry.separation(footprint(), obstacle);
        double length = push.getNorm();
        if (length < 1e-9) {
          continue;
        }
        moved = true;
        touchingNow.add(obstacle);
        double nx = push.getX() / length;
        double ny = push.getY() / length;
        x += push.getX() + nx * 1e-6;
        y += push.getY() + ny * 1e-6;
        double into = vx * nx + vy * ny; // negative while heading into the obstacle
        if (into < 0) {
          if (!touchingLastLoop.contains(obstacle) && -into > BUMP_SPEED) {
            bumps++;
            hardestBump = Math.max(hardestBump, -into);
            Translation2d where = obstacle.nearest(new Translation2d(x, y));
            recentBumps.addLast(new Bump(where.getX(), where.getY(), time, -into));
            while (recentBumps.size() > RECENT_BUMPS) {
              recentBumps.removeFirst();
            }
          }
          vx -= into * nx;
          vy -= into * ny;
        }
      }
      if (!moved) {
        break;
      }
    }

    // Pressed up against something without moving into it this loop still counts as touching it.
    for (Rectangle2d obstacle : obstacles) {
      if (!touchingNow.contains(obstacle) && Geometry.distance(footprint(), obstacle) < 1e-4) {
        touchingNow.add(obstacle);
      }
    }
    touching = !touchingNow.isEmpty();
    if (touching) {
      contactTime += dt;
    }
    touchingLastLoop = touchingNow;
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

  Rectangle2d footprint() {
    return Geometry.robotFootprint(pose());
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

  int bumps() {
    return bumps;
  }

  double hardestBump() {
    return hardestBump;
  }

  double contactTime() {
    return contactTime;
  }

  double distance() {
    return distance;
  }

  boolean touching() {
    return touching;
  }

  int resets() {
    return resets;
  }

  List<Bump> recentBumps() {
    return List.copyOf(recentBumps);
  }
}
