package frc.robot.subsystems.drive;

import edu.wpi.first.math.geometry.Pose2d;
import org.littletonrobotics.junction.AutoLog;

/** The drivetrain's hardware layer: everything the {@link Drive} subsystem reads and commands. */
public interface DriveIO {
  @AutoLog
  public static class DriveIOInputs {
    public Pose2d pose = Pose2d.kZero;
  }

  /** Updates the set of loggable inputs. */
  public default void updateInputs(DriveIOInputs inputs) {}

  /** Field-relative speeds: meters per second, and radians per second counterclockwise. */
  public default void setSpeeds(
      double vxMetersPerSec, double vyMetersPerSec, double omegaRadPerSec) {}
}
