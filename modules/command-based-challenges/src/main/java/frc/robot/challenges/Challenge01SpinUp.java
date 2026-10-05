package frc.robot.challenges;

import frc.robot.lab.CommandChallenge;
import frc.robot.subsystems.Shooter;

/**
 * Challenge 1: Spin Up.
 *
 * <p>Pressing A should start the shooter spinning at {@link Shooter#SHOT_RPM} (and it should keep
 * spinning after A is let go). Pressing B should stop it.
 *
 * <p>Use {@code shooter.runOnce(...)} to make a command that does one thing once, and {@code
 * controller.a().onTrue(...)} to run it when A is pressed.
 */
public class Challenge01SpinUp extends CommandChallenge {
  @Override
  public void configureBindings() {
    // TODO: When A is pressed, set the shooter to Shooter.SHOT_RPM.

    // TODO: When B is pressed, stop the shooter.
  }
}
