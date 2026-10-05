package frc.robot.challenges;

import edu.wpi.first.math.MathUtil;
import frc.robot.commands.AimAtHub;
import frc.robot.lab.CommandChallenge;
import frc.robot.subsystems.Drive;

/**
 * Challenge 12: Write a Command Class.
 *
 * <p>While the driver holds the left trigger (LT), the robot should turn to face the hub and stay
 * facing it, while the left stick still drives it around. When LT is let go, normal driving comes
 * back.
 *
 * <p>Write the {@link AimAtHub} command in {@code commands/AimAtHub.java}. The default driving
 * command and the LT binding are already here.
 */
public class Challenge12AimAtHub extends CommandChallenge {
  @Override
  public void configureBindings() {
    drive.setDefaultCommand(
        drive
            .run(() -> drive.driveFieldRelative(xSpeed(), ySpeed(), rotationSpeed()))
            .withName("Drive with sticks"));

    controller.leftTrigger().whileTrue(new AimAtHub(drive, this::xSpeed, this::ySpeed));
  }

  private double xSpeed() {
    return -MathUtil.applyDeadband(controller.getLeftY(), 0.1) * Drive.MAX_SPEED;
  }

  private double ySpeed() {
    return -MathUtil.applyDeadband(controller.getLeftX(), 0.1) * Drive.MAX_SPEED;
  }

  private double rotationSpeed() {
    return -MathUtil.applyDeadband(controller.getRightX(), 0.1) * Drive.MAX_ANGULAR_SPEED;
  }
}
