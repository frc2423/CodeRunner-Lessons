package frc.robot.challenges;

import frc.robot.lab.CommandChallenge;

/**
 * Challenge 2: Hold to Intake.
 *
 * <p>While the left bumper (LB) is held, the intake should swing out and spin its rollers inward.
 * When LB is let go, the rollers should stop and the intake should swing back in.
 *
 * <p>A command can do something when it starts and something else when it ends: look at {@code
 * intake.startEnd(...)}. Then use {@code whileTrue} so the command only runs while the button is
 * held.
 */
public class Challenge02HoldToIntake extends CommandChallenge {
  @Override
  public void configureBindings() {
    // TODO: Make a command that deploys the intake and spins the rollers at Intake.INTAKE_SPEED
    // when it starts, then stops the rollers and retracts the intake when it ends.

    // TODO: Run it while the left bumper is held.
  }
}
