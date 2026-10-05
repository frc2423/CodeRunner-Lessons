package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.lab.CommandChallenge;
import frc.robot.subsystems.Indexer;
import frc.robot.subsystems.Intake;
import frc.robot.subsystems.Shooter;

/**
 * Challenge 8: Who's in Charge?
 *
 * <p>This robot's code already works... mostly. In the trial, the driver pulls RT to shoot, then
 * (out of habit) taps LB to intake while the shot is still spinning up. The shot never happens!
 *
 * <p>Two commands can't use the same subsystem at the same time. Both of these commands need the
 * indexer, so when LB starts the intake command, the scheduler <b>interrupts</b> the shoot command.
 * Fix it so that:
 *
 * <ol>
 *   <li>a shot can't be interrupted by another command (the LB press is ignored), and
 *   <li>B still cancels a shot that's spinning up, and the shooter and indexer stop when it does.
 * </ol>
 */
public class Challenge08WhosInCharge extends CommandChallenge {
  @Override
  public void configureBindings() {
    Command shoot = shoot();

    controller.rightTrigger().onTrue(shoot);
    controller.leftBumper().whileTrue(intakeUntilLoaded());

    // TODO: Make B cancel the shot.
  }

  /** Spins up, feeds the piece in once the shooter is at speed, then stops the shooter. */
  private Command shoot() {
    // TODO: Don't let other commands interrupt this one. And when it does get cancelled, it
    // should still stop the shooter and the indexer.
    return Commands.sequence(
            shooter.runOnce(() -> shooter.setRPM(Shooter.SHOT_RPM)),
            Commands.waitUntil(shooter::atSpeed),
            indexer
                .runEnd(() -> indexer.setSpeed(Indexer.FEED_SPEED), indexer::stop)
                .until(() -> !indexer.hasPiece()),
            Commands.waitSeconds(0.2),
            shooter.runOnce(shooter::stop))
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
