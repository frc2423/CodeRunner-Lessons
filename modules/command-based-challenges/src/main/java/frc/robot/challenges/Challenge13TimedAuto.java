package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.lab.CommandChallenge;

/**
 * Challenge 13: Timed Autonomous.
 *
 * <p>For the first 15 seconds of a match, the robot drives itself. Write an autonomous routine
 * that:
 *
 * <ol>
 *   <li>shoots the piece the robot starts with,
 *   <li>drives at least 1 m away from where it started, and
 *   <li>stops before autonomous ends.
 * </ol>
 *
 * <p>Make it one long {@code Commands.sequence(...)}. To drive for a while, run a driving command
 * with {@code .withTimeout(seconds)}.
 *
 * <p>Bonus: there's a second piece on the floor 1.7 m in front of you, between you and the hub.
 * Pick it up and shoot it too.
 *
 * <p>To run it, set the Driver Station to Autonomous and click Enable.
 */
public class Challenge13TimedAuto extends CommandChallenge {
  @Override
  public void configureBindings() {
    // Nothing to bind: this challenge is all autonomous.
  }

  @Override
  public Command getAutonomousCommand() {
    // TODO: Return your autonomous routine.
    return Commands.none();
  }
}
