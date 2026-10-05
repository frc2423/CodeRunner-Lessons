package frc.robot.lab;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.subsystems.Drive;
import frc.robot.subsystems.Indexer;
import frc.robot.subsystems.Intake;
import frc.robot.subsystems.Leds;
import frc.robot.subsystems.Shooter;

/**
 * What every challenge builds on. A challenge is a small RobotContainer: it gets the robot's
 * subsystems and the driver's controller, binds commands to buttons and triggers in {@link
 * #configureBindings()}, and can pick an autonomous command.
 *
 * <p>The command lab makes a new challenge object every time you click <b>Run trial</b> or <b>Free
 * play</b>, after clearing every old binding and default command. You don't need to edit this file.
 */
public abstract class CommandChallenge {
  /** The swerve drivetrain. */
  protected final Drive drive;

  /** The intake at the front of the robot: it swings out and its rollers grab game pieces. */
  protected final Intake intake;

  /** The belts that carry a piece from the intake to the shooter, with a beam break sensor. */
  protected final Indexer indexer;

  /** The flywheel that launches pieces at the hub. */
  protected final Shooter shooter;

  /** The robot's LED strips. */
  protected final Leds leds;

  /** The driver's Xbox controller (or keyboard, or the dashboard's on-screen controller). */
  protected final CommandXboxController controller;

  protected CommandChallenge() {
    CommandLab.Parts parts = CommandLab.parts();
    drive = parts.drive();
    intake = parts.intake();
    indexer = parts.indexer();
    shooter = parts.shooter();
    leds = parts.leds();
    controller = parts.controller();
  }

  /** Called once when the challenge starts: bind commands to buttons and set default commands. */
  public abstract void configureBindings();

  /** The command to run in autonomous. */
  public Command getAutonomousCommand() {
    return Commands.none();
  }
}
