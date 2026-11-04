package frc.robot.challenges;

import frc.robot.Constants;
import frc.robot.challenges.processinputs.Shooter;
import frc.robot.challenges.processinputs.ShooterIO;
import frc.robot.challenges.processinputs.ShooterIOSim;
import frc.robot.lab.LabChallenge;

/**
 * Challenge 5: Process Inputs.
 *
 * <p>Your work is in {@code challenges/processinputs/Shooter.java}: its {@code periodic()} has to
 * read the sensors and hand what it read to AdvantageKit. Read {@code ShooterIO} and {@code
 * ShooterIOSim} first. Holding A spins the shooter up, but nothing works until it can read its own
 * speed.
 */
public class Challenge05ProcessInputs extends LabChallenge {
  private final Shooter shooter =
      new Shooter(
          Constants.currentMode == Constants.Mode.REPLAY ? new ShooterIO() {} : new ShooterIOSim());

  @Override
  public void configureBindings() {
    controller.a().whileTrue(shooter.startEnd(() -> shooter.setTargetRpm(3000.0), shooter::stop));
  }
}
