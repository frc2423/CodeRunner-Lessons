package frc.robot.subsystems.drive;

import frc.robot.lab.Hardware;

/** The drivetrain's hardware layer, on the lab's simulated robot. */
public class DriveIOSim implements DriveIO {
  @Override
  public void updateInputs(DriveIOInputs inputs) {
    inputs.pose = Hardware.drive.getPose();
  }

  @Override
  public void setSpeeds(double vxMetersPerSec, double vyMetersPerSec, double omegaRadPerSec) {
    Hardware.drive.setSpeeds(vxMetersPerSec, vyMetersPerSec, omegaRadPerSec);
  }
}
