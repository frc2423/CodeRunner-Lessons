package frc.robot.subsystems.feeder;

import frc.robot.lab.Hardware;

/** The feeder's hardware layer, on the lab's simulated robot. */
public class FeederIOSim implements FeederIO {
  @Override
  public void updateInputs(FeederIOInputs inputs) {
    inputs.hasPiece = Hardware.feeder.hasPiece();
    inputs.appliedVolts = Hardware.feeder.getAppliedVolts();
    inputs.currentAmps = Hardware.feeder.getCurrentAmps();
  }

  @Override
  public void setVoltage(double volts) {
    Hardware.feeder.setVoltage(volts);
  }
}
