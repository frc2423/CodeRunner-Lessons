package frc.robot.challenges;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.drive.Drive;
import java.util.ArrayList;
import java.util.List;
import org.littletonrobotics.junction.Logger;

public class Challenge03Poses extends LabChallenge {
  public static final Pose2d[] WAYPOINTS = {
    new Pose2d(3.0, 3.0, Rotation2d.kZero),
    new Pose2d(6.0, 3.0, Rotation2d.kZero),
    new Pose2d(6.0, 5.0, Rotation2d.kCCW_90deg)
  };

  private static class PoseLogger extends SubsystemBase {
    private final Drive drive;
    private final List<Pose2d> trail = new ArrayList<>();
    private int loops = 0;

    PoseLogger(Drive drive) {
      this.drive = drive;
    }

    @Override
    public void periodic() {
      Logger.recordOutput("Odometry/Robot", drive.getPose());
      Logger.recordOutput("Odometry/Waypoints", WAYPOINTS);

      if (loops++ % 5 == 0) {
        trail.add(drive.getPose());
      }
      Logger.recordOutput("Odometry/Trail", trail.toArray(new Pose2d[0]));
    }
  }

  private final Drive drive = Drive.create();
  private final PoseLogger poseLogger = new PoseLogger(drive);

  @Override
  public void configureBindings() {
    drive.setDefaultCommand(drive.joystickDrive(controller));
  }
}
