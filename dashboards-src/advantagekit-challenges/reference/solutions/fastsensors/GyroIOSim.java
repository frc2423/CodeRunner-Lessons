package frc.robot.challenges.fastsensors;

import frc.robot.lab.Hardware;

public class GyroIOSim implements GyroIO {
  @Override
  public void updateInputs(GyroIOInputs inputs) {
    inputs.yawRateRadPerSec = Hardware.gyro.getYawRateSamples();
    inputs.sampleTimestamps = Hardware.gyro.getSampleTimestamps();
  }
}
