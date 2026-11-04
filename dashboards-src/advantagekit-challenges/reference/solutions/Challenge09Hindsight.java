package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.feeder.Feeder;
import frc.robot.subsystems.flywheel.Flywheel;
import org.littletonrobotics.junction.Logger;

public class Challenge09Hindsight extends LabChallenge {
  private static class ShotAnalysis extends SubsystemBase {
    private final Flywheel flywheel;
    private final Feeder feeder;
    private boolean hadPiece = false;
    private int shots = 0;

    ShotAnalysis(Flywheel flywheel, Feeder feeder) {
      this.flywheel = flywheel;
      this.feeder = feeder;
    }

    @Override
    public void periodic() {
      Logger.recordOutput("Analysis/ErrorRPM", flywheel.getTargetRpm() - flywheel.getRpm());

      if (hadPiece && !feeder.hasPiece()) {
        shots++;
      }
      hadPiece = feeder.hasPiece();
      Logger.recordOutput("Analysis/ShotCount", shots);
    }
  }

  private final Flywheel flywheel = Flywheel.create();
  private final Feeder feeder = Feeder.create();
  private final ShotAnalysis analysis = new ShotAnalysis(flywheel, feeder);

  @Override
  public void configureBindings() {
    controller
        .a()
        .whileTrue(flywheel.startEnd(() -> flywheel.setTargetRpm(3000.0), flywheel::stop));
    controller.x().whileTrue(feeder.feed());
  }
}
