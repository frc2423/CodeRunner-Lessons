package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.feeder.Feeder;
import frc.robot.subsystems.flywheel.Flywheel;

/**
 * Challenge 9: Log It in Hindsight.
 *
 * <p>Your team's practice match is over and the robot is back in its crate. The log is in {@code
 * src/main/deploy/match-logs/practice-match.wpilog}. You wish you'd logged how far the flywheel was
 * from its target. With most logging systems, that data is gone.
 *
 * <p>Because this log holds every input, you can add the output now, replay the match, and get the
 * value at every moment of it. Click <b>Replay the match</b> on the dashboard: no robot needed.
 */
public class Challenge09Hindsight extends LabChallenge {
  private static class ShotAnalysis extends SubsystemBase {
    private final Flywheel flywheel;
    private final Feeder feeder;

    ShotAnalysis(Flywheel flywheel, Feeder feeder) {
      this.flywheel = flywheel;
      this.feeder = feeder;
    }

    @Override
    public void periodic() {
      // TODO: Record flywheel.getTargetRpm() - flywheel.getRpm() at the key "Analysis/ErrorRPM".

      // TODO (bonus): Count the shots. A shot is a loop where the beam break (feeder.hasPiece())
      // has just gone from true to false. Record the count at the key "Analysis/ShotCount".
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
