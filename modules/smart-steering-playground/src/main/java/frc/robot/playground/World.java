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
 * (no faster than {@link PlaygroundConstants#MAX_ACCELERATION}) and moves.
 *
 * <ul>
 *   <li>Obstacles aren't solid: the robot drives straight through them, and every time it drives
 *       into one counts as a bump. That way it's obvious when smart steering didn't keep it out.
 *   <li>The field walls are solid: the robot is pushed back out and loses the part of its speed
 *       heading into the wall, so it slides along it. Hitting one faster than {@link #BUMP_SPEED}
 *       counts as a bump.
 * </ul>
 *
 * <p>You don't need to edit this file.
 */
final class World {
  /** Hitting a wall faster than this counts as a bump. */
  static final double BUMP_SPEED = 0.25;

  private static final List<Rectangle2d> WALLS = walls();

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
  private double insideTime = 0;
  private double distance = 0;
  private int resets = 0;
  private boolean touching = false;
  private boolean[] inside = new boolean[0];
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
    insideTime = 0;
    distance = 0;
    touching = false;
    inside = new boolean[0];
    touchingLastLoop = new HashSet<>();
    recentBumps.clear();
    resets++;
  }

  /**
   * Runs one loop: accelerate toward {@code target} (field-relative), move, and bump.
   *
   * @param obstacles the obstacles, not including the walls (the world knows where those are)
   */
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

    // The walls are solid: push the robot back out of any it overlaps, a few times in case pushing
    // it out of one wall pushed it into another (in a corner).
    Set<Rectangle2d> touchingNow = new HashSet<>();
    for (int pass = 0; pass < 4; pass++) {
      boolean moved = false;
      for (Rectangle2d wall : WALLS) {
        Translation2d push = Geometry.separation(footprint(), wall);
        double length = push.getNorm();
        if (length < 1e-9) {
          continue;
        }
        moved = true;
        touchingNow.add(wall);
        double nx = push.getX() / length;
        double ny = push.getY() / length;
        x += push.getX() + nx * 1e-6;
        y += push.getY() + ny * 1e-6;
        double into = vx * nx + vy * ny; // negative while heading into the wall
        if (into < 0) {
          if (!touchingLastLoop.contains(wall) && -into > BUMP_SPEED) {
            bump(wall, -into);
          }
          vx -= into * nx;
          vy -= into * ny;
        }
      }
      if (!moved) {
        break;
      }
    }
    // Pressed up against a wall without moving into it this loop still counts as touching it.
    for (Rectangle2d wall : WALLS) {
      if (!touchingNow.contains(wall) && Geometry.distance(footprint(), wall) < 1e-4) {
        touchingNow.add(wall);
      }
    }

    // Obstacles aren't solid: the robot drives right through them, so it's easy to see when smart
    // steering didn't keep it out. Every time it drives into one counts as a bump.
    inside = new boolean[obstacles.size()];
    boolean insideAny = false;
    for (int i = 0; i < obstacles.size(); i++) {
      Rectangle2d obstacle = obstacles.get(i);
      if (!Geometry.overlaps(footprint(), obstacle)) {
        continue;
      }
      inside[i] = true;
      insideAny = true;
      touchingNow.add(obstacle);
      if (!touchingLastLoop.contains(obstacle)) {
        bump(obstacle, Math.hypot(vx, vy));
      }
    }

    touching = !touchingNow.isEmpty();
    if (insideAny) {
      insideTime += dt;
    }
    touchingLastLoop = touchingNow;
    distance += Math.hypot(x - startX, y - startY);
  }

  private void bump(Rectangle2d what, double speed) {
    bumps++;
    hardestBump = Math.max(hardestBump, speed);
    Translation2d where = what.nearest(new Translation2d(x, y));
    recentBumps.addLast(new Bump(where.getX(), where.getY(), time, speed));
    while (recentBumps.size() > RECENT_BUMPS) {
      recentBumps.removeFirst();
    }
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

  /** How long the robot has spent inside obstacles. */
  double insideTime() {
    return insideTime;
  }

  /** Whether the robot overlaps each obstacle, in the order the last {@link #step} got them. */
  boolean[] inside() {
    return inside.clone();
  }

  double distance() {
    return distance;
  }

  /** Whether the robot is inside an obstacle or against a wall. */
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
