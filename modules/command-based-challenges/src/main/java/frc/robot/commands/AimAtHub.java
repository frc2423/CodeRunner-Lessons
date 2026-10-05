package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.Drive;
import java.util.function.DoubleSupplier;

/**
 * Turns the robot to face the hub, while the driver keeps control of driving around. You write this
 * command in challenge 12.
 *
 * <p>Most commands are made with factories like {@code run(...)}, but you can also write a whole
 * class that extends {@link Command}. The scheduler calls its methods:
 *
 * <ul>
 *   <li>{@link #initialize()} once, when the command starts,
 *   <li>{@link #execute()} every loop while it runs,
 *   <li>{@link #isFinished()} after each execute: return true to end the command,
 *   <li>{@link #end(boolean)} once, when it ends or is interrupted.
 * </ul>
 */
public class AimAtHub extends Command {
  private final Drive drive;
  private final DoubleSupplier xSpeed;
  private final DoubleSupplier ySpeed;

  // TODO: Add a PIDController that turns the robot's heading error (in radians) into a turning
  // speed (in radians per second).

  /**
   * @param drive the drivetrain
   * @param xSpeed the driver's field-relative forward speed, in m/s
   * @param ySpeed the driver's field-relative sideways speed, in m/s
   */
  public AimAtHub(Drive drive, DoubleSupplier xSpeed, DoubleSupplier ySpeed) {
    this.drive = drive;
    this.xSpeed = xSpeed;
    this.ySpeed = ySpeed;
    // TODO: This command drives the robot, so it must require the drive.

    setName("Aim at hub");
  }

  @Override
  public void initialize() {
    // TODO (maybe): reset the PID controller.
  }

  @Override
  public void execute() {
    // TODO: Work out the turning speed from drive.getRotation() and drive.getAngleToHub(), then
    // drive field-relative with the driver's x and y speeds and your turning speed.
  }

  @Override
  public boolean isFinished() {
    return false; // keep aiming until the driver lets go
  }

  @Override
  public void end(boolean interrupted) {
    // TODO: Stop the robot.
  }
}
