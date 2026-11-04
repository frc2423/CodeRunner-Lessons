package frc.robot.challenges.nopeeking;

import frc.robot.lab.Hardware;

/** The auto feeder's IO layer on the lab's robot. */
public class AutoFeederIOSim implements AutoFeederIO {
  @Override
  public void updateInputs(AutoFeederIOInputs inputs) {
    inputs.flywheelVelocityRadPerSec = Hardware.flywheel.getVelocityRadPerSec();
    inputs.appliedVolts = Hardware.feeder.getAppliedVolts();
  }

  @Override
  public void setVoltage(double volts) {
    Hardware.feeder.setVoltage(volts);
  }
}
