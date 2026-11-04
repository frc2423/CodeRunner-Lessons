package frc.robot.challenges.nopeeking;

import frc.robot.lab.Hardware;

public class AutoFeederIOSim implements AutoFeederIO {
  @Override
  public void updateInputs(AutoFeederIOInputs inputs) {
    inputs.flywheelVelocityRadPerSec = Hardware.flywheel.getVelocityRadPerSec();
    inputs.appliedVolts = Hardware.feeder.getAppliedVolts();
    inputs.hasPiece = Hardware.feeder.hasPiece();
  }

  @Override
  public void setVoltage(double volts) {
    Hardware.feeder.setVoltage(volts);
  }
}
