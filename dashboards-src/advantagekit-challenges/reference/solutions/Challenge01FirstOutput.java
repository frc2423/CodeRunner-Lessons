package frc.robot.challenges;

import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.flywheel.Flywheel;
import org.littletonrobotics.junction.Logger;

public class Challenge01FirstOutput extends LabChallenge {
  private final Flywheel flywheel = Flywheel.create();

  @Override
  public void configureBindings() {
    controller
        .a()
        .onTrue(
            flywheel.runOnce(
                () -> {
                  flywheel.setTargetRpm(3000.0);
                  Logger.recordOutput("Flywheel/SetpointRPM", 3000.0, "rpm");
                }));

    controller
        .b()
        .onTrue(
            flywheel.runOnce(
                () -> {
                  flywheel.stop();
                  Logger.recordOutput("Flywheel/SetpointRPM", 0.0, "rpm");
                }));
  }
}
