package frc.robot.challenges.processinputs;

import frc.robot.lab.Hardware;

/**
 * The shooter's IO layer on the lab's robot. On a real robot you'd have one of these that talks to
 * a real motor controller instead, and nothing else in the code would change.
 */
public class ShooterIOSim implements ShooterIO {
  @Override
  public void updateInputs(ShooterIOInputs inputs) {
    inputs.velocityRadPerSec = Hardware.flywheel.getVelocityRadPerSec();
    inputs.appliedVolts = Hardware.flywheel.getAppliedVolts();
  }

  @Override
  public void setVoltage(double volts) {
    Hardware.flywheel.setVoltage(volts);
  }
}
