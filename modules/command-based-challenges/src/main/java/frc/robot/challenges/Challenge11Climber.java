package frc.robot.challenges;

import frc.robot.lab.CommandChallenge;
import frc.robot.subsystems.Climber;

/**
 * Challenge 11: Write a Subsystem.
 *
 * <p>The robot has a climber, but no code for it yet. Write the {@link Climber} subsystem in {@code
 * subsystems/Climber.java}: its {@code extend()} and {@code climb()} commands. Then bind them here:
 *
 * <ul>
 *   <li>up on the d-pad: extend the hook to the top, then stop,
 *   <li>down on the d-pad: pull the robot up (the hook goes down) until the bottom limit switch,
 *       then stop.
 * </ul>
 *
 * <p>Pressing down while the hook is still going up must reverse it straight away. Never drive the
 * motor into either end of its travel.
 *
 * <p>The robot starts right under the tower's bar, so it can hook on.
 */
public class Challenge11Climber extends CommandChallenge {
  private final Climber climber = new Climber();

  @Override
  public void configureBindings() {
    // TODO: Up on the d-pad (controller.povUp()) runs climber.extend().

    // TODO: Down on the d-pad runs climber.climb().
  }
}
