package frc.robot.challenges.fromscratch;

import frc.robot.lab.Hardware;

public class PivotIOSim implements PivotIO {
  @Override
  public void updateInputs(PivotIOInputs inputs) {
    inputs.positionRad = Hardware.pivot.getPositionRad();
    inputs.velocityRadPerSec = Hardware.pivot.getVelocityRadPerSec();
    inputs.appliedVolts = Hardware.pivot.getAppliedVolts();
  }

  @Override
  public void setVoltage(double volts) {
    Hardware.pivot.setVoltage(volts);
  }
}
