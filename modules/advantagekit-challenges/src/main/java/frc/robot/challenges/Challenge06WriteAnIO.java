package frc.robot.challenges;

import frc.robot.Constants;
import frc.robot.challenges.ioexercise.Intake;
import frc.robot.challenges.ioexercise.IntakeIO;
import frc.robot.challenges.ioexercise.IntakeIOSim;
import frc.robot.lab.LabChallenge;

/**
 * Challenge 6: Write an IO Layer.
 *
 * <p>Your work is in {@code challenges/ioexercise/}. Write the whole IO layer for the robot's
 * intake rollers, in this order: {@code IntakeIO.java}, then {@code IntakeIOSim.java}, then finish
 * {@code Intake.java}. The command below is done: holding A runs the rollers.
 */
public class Challenge06WriteAnIO extends LabChallenge {
  private final Intake intake =
      new Intake(
          Constants.currentMode == Constants.Mode.REPLAY ? new IntakeIO() {} : new IntakeIOSim());

  @Override
  public void configureBindings() {
    controller
        .a()
        .whileTrue(intake.startEnd(() -> intake.setVolts(6.0), () -> intake.setVolts(0.0)));
  }
}
