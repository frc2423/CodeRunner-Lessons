package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.lab.CommandChallenge;
import frc.robot.subsystems.Indexer;
import frc.robot.subsystems.Intake;
import frc.robot.subsystems.Shooter;

/**
 * Challenge 10: Smart Triggers.
 *
 * <p>Triggers can be combined with {@code and}, {@code or} and {@code negate}, and smoothed out
 * with {@code debounce}. Use them to make the controls smarter:
 *
 * <ol>
 *   <li>RT shoots, but only when a piece is loaded. With nothing loaded, RT does nothing.
 *   <li>LB intakes (the command is written for you).
 *   <li>Anti-jam: sometimes a piece jams in the intake. When that happens, the roller motor's
 *       current stays high. If it stays above 30 amps for a quarter of a second, run the rollers
 *       backwards for 0.3 seconds to clear the jam. (A normal pickup makes the current jump for a
 *       moment too, so make sure that doesn't set it off.)
 * </ol>
 *
 * <p>Bonus: when RT is pulled with nothing loaded, flash the LEDs red for half a second, then turn
 * them off.
 */
public class Challenge10SmartTriggers extends CommandChallenge {
  @Override
  public void configureBindings() {
    controller.leftBumper().whileTrue(intakeUntilLoaded());

    // TODO: RT shoots only when a piece is loaded.

    // TODO: Make a Trigger that's true when the intake's roller current is above 30 A, debounce
    // it, and run unjam() when it becomes true.

    // TODO (bonus): RT with nothing loaded flashes the LEDs red.
  }

  /** Runs the intake rollers backwards for 0.3 s. */
  private Command unjam() {
    // TODO
    return Commands.none();
  }

  /** Spins up, feeds the piece in once the shooter is at speed, then stops. */
  private Command shoot() {
    return Commands.sequence(
            shooter.runOnce(() -> shooter.setRPM(Shooter.SHOT_RPM)),
            Commands.waitUntil(shooter::atSpeed),
            indexer
                .runEnd(() -> indexer.setSpeed(Indexer.FEED_SPEED), indexer::stop)
                .until(() -> !indexer.hasPiece()),
            Commands.waitSeconds(0.2))
        .finallyDo(
            () -> {
              shooter.stop();
              indexer.stop();
            })
        .withName("Shoot");
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
