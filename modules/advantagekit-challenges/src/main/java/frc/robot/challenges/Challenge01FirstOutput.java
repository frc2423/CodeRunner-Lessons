package frc.robot.challenges;

import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.flywheel.Flywheel;

/**
 * Challenge 1: Your First Output.
 *
 * <p>The buttons already work: A spins the flywheel up to 3000 RPM, and B stops it. But nobody can
 * tell what the robot was asked to do, because nothing writes it down.
 *
 * <p>Record what the driver asked for as an output, every time it changes.
 */
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
                  // TODO: Record 3000.0 as an output at the key "Flywheel/SetpointRPM".
                }));

    controller
        .b()
        .onTrue(
            flywheel.runOnce(
                () -> {
                  flywheel.stop();
                  // TODO: Record 0.0 at the same key.
                }));
  }
}
