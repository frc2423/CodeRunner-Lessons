package frc.robot;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rectangle2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import java.util.List;

/**
 * The obstacles on the field, written in code. They show up on the Playground tab in gray. They
 * aren't solid: the robot can drive through them, and each time it does counts as a bump. You can
 * also draw obstacles on the tab with the mouse (they show up in orange), and <b>Copy as code</b>
 * turns the ones you drew into lines you can paste in here.
 *
 * <p>A {@link Rectangle2d} can be made two ways:
 *
 * <ul>
 *   <li>from two opposite corners: {@code new Rectangle2d(new Translation2d(x1, y1), new
 *       Translation2d(x2, y2))}, or
 *   <li>from its center, which can be rotated, and its size along its own X and Y: {@code new
 *       Rectangle2d(new Pose2d(x, y, Rotation2d.fromDegrees(30)), xWidth, yWidth)}.
 * </ul>
 *
 * <p>The field is {@code 16.54} m long (X) and {@code 8.07} m wide (Y), with (0, 0) in the corner
 * to the right of the blue driver station. Save and restart the robot program to see changes.
 */
public final class Obstacles {
  private Obstacles() {}

  /** Called once when the robot program starts. */
  public static List<Rectangle2d> create() {
    return List.of(
        // A long wall across the middle of the left half.
        new Rectangle2d(new Translation2d(4.5, 1.5), new Translation2d(5.0, 6.5)),
        // A box on its own.
        new Rectangle2d(new Translation2d(8.0, 3.2), new Translation2d(9.2, 4.8)),
        // A tilted barrier.
        new Rectangle2d(new Pose2d(12.0, 6.0, Rotation2d.fromDegrees(35)), 3.0, 0.4),
        // A narrow gap between two blocks: can you squeeze through?
        new Rectangle2d(new Translation2d(12.0, 0.0), new Translation2d(12.8, 2.6)),
        new Rectangle2d(new Translation2d(12.0, 3.6), new Translation2d(12.8, 4.4)));
  }
}
