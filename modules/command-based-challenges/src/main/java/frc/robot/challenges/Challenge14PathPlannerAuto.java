package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.lab.CommandChallenge;

/**
 * Challenge 14: PathPlanner Autonomous.
 *
 * <p>Timed driving is hard to get right. PathPlanner lets you draw the robot's path on the field
 * instead, and run your own commands at points along it.
 *
 * <ol>
 *   <li>Register named commands, so PathPlanner autos can use them: "Intake" (intake until loaded)
 *       and "Shoot".
 *   <li>Open the <b>PathPlanner</b> tab. The auto "Two Piece" drives to the piece at the top and
 *       back to the shooting spot, but it doesn't shoot or intake yet. Add your named commands to
 *       it.
 *   <li>Return the auto from {@link #getAutonomousCommand()}.
 * </ol>
 *
 * <p>Bonus: make a "Three Piece" auto that also grabs the piece at the bottom.
 *
 * <p>The PathPlanner tab saves into {@code src/main/deploy/pathplanner/}. Click Restart in the
 * Driver Station after changing an auto, so the robot program loads it again.
 */
public class Challenge14PathPlannerAuto extends CommandChallenge {
  public Challenge14PathPlannerAuto() {
    // TODO: Register the "Intake" and "Shoot" named commands with NamedCommands.registerCommand.
  }

  @Override
  public void configureBindings() {
    // Nothing to bind: this challenge is all autonomous.
  }

  @Override
  public Command getAutonomousCommand() {
    // TODO: Return the "Two Piece" auto.
    return Commands.none();
  }
}
