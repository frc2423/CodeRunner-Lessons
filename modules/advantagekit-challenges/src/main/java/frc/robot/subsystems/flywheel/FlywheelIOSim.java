package frc.robot.subsystems.flywheel;

import frc.robot.lab.Hardware;

/** The flywheel's hardware layer, on the lab's simulated robot. */
public class FlywheelIOSim implements FlywheelIO {
  @Override
  public void updateInputs(FlywheelIOInputs inputs) {
    inputs.velocityRadPerSec = Hardware.flywheel.getVelocityRadPerSec();
    inputs.appliedVolts = Hardware.flywheel.getAppliedVolts();
    inputs.currentAmps = Hardware.flywheel.getCurrentAmps();
  }

  @Override
  public void setVoltage(double volts) {
    Hardware.flywheel.setVoltage(volts);
  }
}
