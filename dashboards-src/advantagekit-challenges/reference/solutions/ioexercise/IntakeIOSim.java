package frc.robot.challenges.ioexercise;

import frc.robot.lab.Hardware;

public class IntakeIOSim implements IntakeIO {
  @Override
  public void updateInputs(IntakeIOInputs inputs) {
    inputs.hasPiece = Hardware.feeder.hasPiece();
    inputs.appliedVolts = Hardware.feeder.getAppliedVolts();
    inputs.currentAmps = Hardware.feeder.getCurrentAmps();
  }

  @Override
  public void setVoltage(double volts) {
    Hardware.feeder.setVoltage(volts);
  }
}
