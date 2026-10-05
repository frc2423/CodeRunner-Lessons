package frc.robot.challenges;

import frc.robot.lab.CommandChallenge;

/**
 * Challenge 7: Ready, Set, Shoot.
 *
 * <p>Waiting a whole second for the shooter after picking up a piece wastes time. While the right
 * bumper (RB) is held, the robot should:
 *
 * <ul>
 *   <li>intake (like challenge 5) <i>and at the same time</i> spin up the shooter,
 *   <li>as soon as the piece is loaded and the shooter is at speed, feed it in and shoot,
 *   <li>then stop everything.
 * </ul>
 *
 * <p>Letting go of RB at any point should stop everything.
 *
 * <p>Commands can run side by side in a group: look at {@code Commands.parallel}, {@code
 * Commands.deadline} and {@code Commands.race}, and the decorators {@code alongWith}, {@code
 * deadlineFor} and {@code raceWith}.
 */
public class Challenge07ReadySetShoot extends CommandChallenge {
  @Override
  public void configureBindings() {
    // TODO: Build "intake until loaded" and "feed until the piece is gone" commands.

    // TODO: Run them one after the other, with the shooter spinning the whole time.

    // TODO: Run that while RB is held.
  }
}
