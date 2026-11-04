package frc.robot.challenges.mystery;

import frc.robot.lab.Hardware;

/** The shooter's IO layer on the lab's robot. */
public class ShooterIOSim implements ShooterIO {
  @Override
  public void updateInputs(ShooterIOInputs inputs) {
    inputs.flywheelRadPerSec = Hardware.flywheel.getVelocityRadPerSec();
    inputs.flywheelVolts = Hardware.flywheel.getAppliedVolts();
    inputs.feederVolts = Hardware.feeder.getAppliedVolts();
    inputs.hasPiece = Hardware.feeder.hasPiece();
  }

  @Override
  public void setFlywheelVoltage(double volts) {
    Hardware.flywheel.setVoltage(volts);
  }

  @Override
  public void setFeederVoltage(double volts) {
    Hardware.feeder.setVoltage(volts);
  }
}
