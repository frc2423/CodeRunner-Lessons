package frc.robot.playground;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rectangle2d;
import edu.wpi.first.math.geometry.Translation2d;

/**
 * Rectangle math for the reference solution and the calibration test. Kept out of the module: students
 * write their own.
 */
final class RefGeometry {
  private RefGeometry() {}

  /** The robot's bumpers as a rectangle, if the robot were at {@code pose}. */
  static Rectangle2d robotFootprint(Pose2d pose) {
    return new Rectangle2d(pose, PlaygroundConstants.ROBOT_LENGTH, PlaygroundConstants.ROBOT_WIDTH);
  }

  /** The four corners of a rectangle, counterclockwise. Works for rotated rectangles too. */
  static Translation2d[] corners(Rectangle2d rectangle) {
    Pose2d center = rectangle.getCenter();
    double halfX = rectangle.getXWidth() / 2;
    double halfY = rectangle.getYWidth() / 2;
    return new Translation2d[] {
      offset(center, halfX, halfY),
      offset(center, -halfX, halfY),
      offset(center, -halfX, -halfY),
      offset(center, halfX, -halfY),
    };
  }

  /** Whether two rectangles overlap. Rectangles that only touch along an edge don't count. */
  static boolean overlaps(Rectangle2d a, Rectangle2d b) {
    return penetration(a, b) != null;
  }

  /**
   * The shortest move that would push rectangle {@code a} out of rectangle {@code b}, or (0, 0) if
   * they don't overlap. Its direction is the way {@code a} would have to go to escape, which is
   * also the direction the surface of {@code b} faces where they touch.
   */
  static Translation2d separation(Rectangle2d a, Rectangle2d b) {
    Translation2d push = penetration(a, b);
    return push == null ? Translation2d.kZero : push;
  }

  /**
   * The gap between two rectangles: how far apart their closest points are, or 0 if they overlap.
   */
  static double distance(Rectangle2d a, Rectangle2d b) {
    if (overlaps(a, b)) {
      return 0;
    }
    // For two separate convex shapes, the closest pair of points always includes a corner.
    double best = Double.POSITIVE_INFINITY;
    for (Translation2d corner : corners(a)) {
      best = Math.min(best, b.getDistance(corner));
    }
    for (Translation2d corner : corners(b)) {
      best = Math.min(best, a.getDistance(corner));
    }
    return best;
  }

  // The separating axis test ---------------------------------------------------------------------

  private static Translation2d offset(Pose2d center, double x, double y) {
    return center.getTranslation().plus(new Translation2d(x, y).rotateBy(center.getRotation()));
  }

  /**
   * Separating axis test. Two convex shapes don't overlap exactly when some axis (here, one of the
   * rectangles' edge directions) has their shadows on it apart. If every axis overlaps, the axis
   * with the smallest overlap is the quickest way out.
   */
  private static Translation2d penetration(Rectangle2d a, Rectangle2d b) {
    Translation2d[] cornersA = corners(a);
    Translation2d[] cornersB = corners(b);
    double angleA = a.getRotation().getRadians();
    double angleB = b.getRotation().getRadians();
    double[][] axes = {
      {Math.cos(angleA), Math.sin(angleA)},
      {-Math.sin(angleA), Math.cos(angleA)},
      {Math.cos(angleB), Math.sin(angleB)},
      {-Math.sin(angleB), Math.cos(angleB)},
    };

    double bestOverlap = Double.POSITIVE_INFINITY;
    double bestX = 0;
    double bestY = 0;
    for (double[] axis : axes) {
      double[] shadowA = shadow(cornersA, axis);
      double[] shadowB = shadow(cornersB, axis);
      double overlap = Math.min(shadowA[1], shadowB[1]) - Math.max(shadowA[0], shadowB[0]);
      if (overlap <= 1e-9) {
        return null;
      }
      if (overlap < bestOverlap) {
        bestOverlap = overlap;
        // Push a away from b: compare the middles of the two shadows.
        double sign = (shadowA[0] + shadowA[1]) >= (shadowB[0] + shadowB[1]) ? 1 : -1;
        bestX = axis[0] * sign;
        bestY = axis[1] * sign;
      }
    }
    return new Translation2d(bestX * bestOverlap, bestY * bestOverlap);
  }

  private static double[] shadow(Translation2d[] corners, double[] axis) {
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    for (Translation2d corner : corners) {
      double d = corner.getX() * axis[0] + corner.getY() * axis[1];
      min = Math.min(min, d);
      max = Math.max(max, d);
    }
    return new double[] {min, max};
  }
}
