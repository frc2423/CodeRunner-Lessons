package frc.robot.challenges;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.drive.Drive;

/**
 * Challenge 3: Log a Pose.
 *
 * <p>AdvantageKit can log whole objects, like a {@code Pose2d}, not just numbers. AdvantageScope
 * knows what they are, so it can draw them on a field. So can this lab: the field on the dashboard
 * only draws what your code logs, so it starts out empty.
 *
 * <p>Fill in {@link PoseLogger#periodic()}.
 */
public class Challenge03Poses extends LabChallenge {
  /** The places the robot should visit. */
  public static final Pose2d[] WAYPOINTS = {
    new Pose2d(3.0, 3.0, Rotation2d.kZero),
    new Pose2d(6.0, 3.0, Rotation2d.kZero),
    new Pose2d(6.0, 5.0, Rotation2d.kCCW_90deg)
  };

  private static class PoseLogger extends SubsystemBase {
    private final Drive drive;

    PoseLogger(Drive drive) {
      this.drive = drive;
    }

    @Override
    public void periodic() {
      // TODO: Record drive.getPose() at the key "Odometry/Robot".

      // TODO: Record WAYPOINTS at the key "Odometry/Waypoints".
    }
  }

  private final Drive drive = Drive.create();
  private final PoseLogger poseLogger = new PoseLogger(drive);

  @Override
  public void configureBindings() {
    drive.setDefaultCommand(drive.joystickDrive(controller));
  }
}
