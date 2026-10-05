package frc.robot.lab;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.subsystems.Drive;

/** Things the lab adds in free play so you can move around, even before you've written them. */
final class FreePlay {
  private FreePlay() {}

  /** Field-relative driving on the sticks, for challenges that aren't about driving. */
  static Command drive(Drive drive, CommandXboxController controller) {
    return drive
        .run(
            () ->
                drive.driveFieldRelative(
                    -MathUtil.applyDeadband(controller.getLeftY(), 0.1) * Drive.MAX_SPEED,
                    -MathUtil.applyDeadband(controller.getLeftX(), 0.1) * Drive.MAX_SPEED,
                    -MathUtil.applyDeadband(controller.getRightX(), 0.1) * Drive.MAX_ANGULAR_SPEED))
        .withName("Lab: free-play driving");
  }
}
