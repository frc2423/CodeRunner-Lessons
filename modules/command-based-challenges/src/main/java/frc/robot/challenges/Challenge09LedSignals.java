package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.lab.CommandChallenge;
import frc.robot.subsystems.Indexer;
import frc.robot.subsystems.Intake;
import frc.robot.subsystems.Shooter;

/**
 * Challenge 9: LED Signals.
 *
 * <p>The drive team can't see inside the robot, so the LEDs tell them what's going on:
 *
 * <ul>
 *   <li>no piece: the alliance color (red or blue, from {@code DriverStation.getAlliance()}),
 *   <li>holding a piece: green,
 *   <li>shooter at speed: white (ready to shoot!).
 * </ul>
 *
 * <p>A {@link edu.wpi.first.wpilibj2.command.button.Trigger} doesn't have to be a button: {@code
 * new Trigger(indexer::hasPiece)} is true whenever the beam break sees a piece. Bind LED commands
 * to triggers like that, and give the LEDs a default command for when nothing else is using them.
 *
 * <p>The intake (LB) and the shooter buttons (A spins up, B stops) are already written.
 */
public class Challenge09LedSignals extends CommandChallenge {
  @Override
  public void configureBindings() {
    controller.leftBumper().whileTrue(intakeUntilLoaded());
    controller.a().onTrue(shooter.runOnce(() -> shooter.setRPM(Shooter.SHOT_RPM)));
    controller.b().onTrue(shooter.runOnce(shooter::stop));

    // TODO: Give the LEDs a default command that shows the alliance color.

    // TODO: Make the LEDs green while there's a piece in the robot.

    // TODO: Make the LEDs white while the shooter is at speed.

    // TODO (bonus): Rumble the controller for half a second when a piece loads.
  }

  /** Deploys the intake and runs it and the indexer until a piece reaches the beam break. */
  private Command intakeUntilLoaded() {
    return Commands.runEnd(
            () -> {
              intake.deploy();
              intake.setRollers(Intake.INTAKE_SPEED);
              indexer.setSpeed(Indexer.LOAD_SPEED);
            },
            () -> {
              intake.stopRollers();
              intake.retract();
              indexer.stop();
            },
            intake,
            indexer)
        .until(indexer::hasPiece)
        .withName("Intake");
  }
}
