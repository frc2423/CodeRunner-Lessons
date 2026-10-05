package frc.robot.challenges;

import frc.robot.lab.CommandChallenge;

/**
 * Challenge 5: Intake Until Loaded.
 *
 * <p>Holding LB should pick up a game piece: deploy the intake, run its rollers, and run the
 * indexer so it pulls the piece into the robot. As soon as the indexer's beam break sees the piece,
 * the command should end by itself (stop everything and retract the intake), even if the driver is
 * still holding LB. Otherwise the indexer pushes the piece right on into the shooter!
 *
 * <p>This command needs two subsystems: the intake and the indexer. A command can require more than
 * one.
 */
public class Challenge05IntakeUntilLoaded extends CommandChallenge {
  @Override
  public void configureBindings() {
    // TODO: Make a command that requires the intake AND the indexer. While it runs: deploy the
    // intake, spin the rollers, run the indexer. When it ends: stop both, retract the intake.

    // TODO: Make it end as soon as indexer.hasPiece() is true.

    // TODO: Run it while LB is held.
  }
}
