package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.feeder.Feeder;
import frc.robot.subsystems.flywheel.Flywheel;
import org.littletonrobotics.junction.Logger;

// NAIVE: a loop that never ends. The replay can't finish, and the lab has to say so and recover.
public class Challenge09Hindsight extends LabChallenge {
  private static class ShotAnalysis extends SubsystemBase {
    private final Flywheel flywheel;

    ShotAnalysis(Flywheel flywheel) {
      this.flywheel = flywheel;
    }

    @Override
    public void periodic() {
      while (flywheel.getTargetRpm() >= 0) {
        Logger.recordOutput("Analysis/ErrorRPM", flywheel.getTargetRpm() - flywheel.getRpm());
      }
    }
  }

  private final Flywheel flywheel = Flywheel.create();
  private final Feeder feeder = Feeder.create();
  private final ShotAnalysis analysis = new ShotAnalysis(flywheel);

  @Override
  public void configureBindings() {
    controller
        .a()
        .whileTrue(flywheel.startEnd(() -> flywheel.setTargetRpm(3000.0), flywheel::stop));
    controller.x().whileTrue(feeder.feed());
  }
}
