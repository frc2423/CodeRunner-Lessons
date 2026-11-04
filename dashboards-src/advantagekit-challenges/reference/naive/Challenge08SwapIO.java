package frc.robot.challenges;

import frc.robot.Constants;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.flywheel.Flywheel;
import frc.robot.subsystems.flywheel.FlywheelIO;
import frc.robot.subsystems.flywheel.FlywheelIOSim;
import frc.robot.subsystems.flywheel.FlywheelIOSpark;

// NAIVE: gives a replay the simulated hardware.
public class Challenge08SwapIO extends LabChallenge {
  private final Flywheel flywheel = new Flywheel(createIO(Constants.currentMode));

  public static FlywheelIO createIO(Constants.Mode mode) {
    return switch (mode) {
      case REAL -> new FlywheelIOSpark();
      case SIM -> new FlywheelIOSim();
      case REPLAY -> new FlywheelIOSim();
    };
  }

  @Override
  public void configureBindings() {
    controller
        .a()
        .whileTrue(flywheel.startEnd(() -> flywheel.setTargetRpm(3000.0), flywheel::stop));
  }
}
