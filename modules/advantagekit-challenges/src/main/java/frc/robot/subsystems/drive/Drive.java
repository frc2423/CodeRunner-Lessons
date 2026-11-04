package frc.robot.subsystems.drive;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.Constants;
import org.littletonrobotics.junction.Logger;

/** A simple field-relative drivetrain. */
public class Drive extends SubsystemBase {
  private static final double MAX_SPEED_METERS_PER_SEC = 3.0;
  private static final double MAX_TURN_RAD_PER_SEC = 3.0;
  private static final double DEADBAND = 0.1;

  private final DriveIO io;
  private final DriveIOInputsAutoLogged inputs = new DriveIOInputsAutoLogged();

  /** A drivetrain with the right IO for how the program is running. */
  public static Drive create() {
    return new Drive(
        switch (Constants.currentMode) {
          case REPLAY -> new DriveIO() {};
          default -> new DriveIOSim();
        });
  }

  public Drive(DriveIO io) {
    this.io = io;
  }

  @Override
  public void periodic() {
    io.updateInputs(inputs);
    Logger.processInputs("Drive", inputs);
  }

  /** Where the robot is on the field. */
  public Pose2d getPose() {
    return inputs.pose;
  }

  public void drive(double vxMetersPerSec, double vyMetersPerSec, double omegaRadPerSec) {
    io.setSpeeds(vxMetersPerSec, vyMetersPerSec, omegaRadPerSec);
  }

  /** Drives with the controller's sticks: left stick moves, right stick turns. */
  public Command joystickDrive(CommandXboxController controller) {
    return run(
        () ->
            drive(
                -MathUtil.applyDeadband(controller.getLeftY(), DEADBAND) * MAX_SPEED_METERS_PER_SEC,
                -MathUtil.applyDeadband(controller.getLeftX(), DEADBAND) * MAX_SPEED_METERS_PER_SEC,
                -MathUtil.applyDeadband(controller.getRightX(), DEADBAND) * MAX_TURN_RAD_PER_SEC));
  }
}
