package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.lab.CommandChallenge;

/**
 * Challenge 15: Match Play.
 *
 * <p>Put it all together. Write the controls you'd want in a real match, then play a one-minute
 * match yourself: with the keyboard (click the Command Lab tab first), the tab's on-screen
 * controller, or a gamepad. Score at least 3 pieces to pass.
 *
 * <p>Ideas, from the earlier challenges:
 *
 * <ul>
 *   <li>a default command for driving (challenge 4),
 *   <li>intake until loaded (5) and shoot (6), or both in one button (7),
 *   <li>LEDs that tell you what's going on (9) and smart triggers (10),
 *   <li>aim at the hub (12), so you can shoot from anywhere with {@code
 *       Shooter.rpmForDistance(drive.getDistanceToHub())},
 *   <li>the climber (11): hang from the tower at the end for a bonus.
 * </ul>
 *
 * <p>Copy what you need from your other challenge files. The human player rolls in a new piece near
 * your driver station every few seconds.
 */
public class Challenge15MatchPlay extends CommandChallenge {
  @Override
  public void configureBindings() {
    // TODO: Your robot's controls.
  }

  @Override
  public Command getAutonomousCommand() {
    // Optional: your best autonomous routine (not graded here).
    return Commands.none();
  }
}
