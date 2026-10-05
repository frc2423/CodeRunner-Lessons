package frc.robot.challenges;

import frc.robot.lab.CommandChallenge;

/**
 * Challenge 6: Shoot!
 *
 * <p>The robot starts at the shooting spot holding a piece. Pulling the right trigger (RT) should
 * shoot it, in steps, one after the other:
 *
 * <ol>
 *   <li>start the shooter spinning at {@code Shooter.SHOT_RPM},
 *   <li>wait until the shooter is at speed,
 *   <li>run the indexer to feed the piece into the shooter, until it's gone,
 *   <li>stop the shooter.
 * </ol>
 *
 * <p>Build it with {@code Commands.sequence(...)} (or {@code andThen}). One quick pull of RT should
 * be enough: the whole sequence runs by itself.
 */
public class Challenge06Shoot extends CommandChallenge {
  @Override
  public void configureBindings() {
    // TODO: Make the shoot command as a sequence of smaller commands.

    // TODO: Run it when RT is pulled (controller.rightTrigger()).
  }
}
