package frc.robot.challenges;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.flywheel.Flywheel;
import java.util.Random;
import org.littletonrobotics.junction.Logger;

public class Challenge10Deterministic extends LabChallenge {
  private static class WarmUp extends SubsystemBase {
    private final Flywheel flywheel;
    private final Random random = new Random(1234);
    private boolean running = false;
    private double startSeconds = 0;

    WarmUp(Flywheel flywheel) {
      this.flywheel = flywheel;
    }

    void begin() {
      running = true;
      startSeconds = Timer.getTimestamp();
    }

    void end() {
      running = false;
      flywheel.stop();
    }

    @Override
    public void periodic() {
      if (!running) {
        return;
      }
      double elapsed = Timer.getTimestamp() - startSeconds;
      double base = 1000.0 + Math.min(elapsed, 2.0) * 1000.0;
      double wobble = (random.nextDouble() - 0.5) * 100.0;

      flywheel.setTargetRpm(base + wobble);
      Logger.recordOutput("WarmUp/ElapsedSeconds", elapsed);
      Logger.recordOutput("WarmUp/WobbleRPM", wobble);
      Logger.recordOutput("WarmUp/TargetRPM", base + wobble);
    }
  }

  private final Flywheel flywheel = Flywheel.create();
  private final WarmUp warmUp = new WarmUp(flywheel);

  @Override
  public void configureBindings() {
    controller.a().whileTrue(warmUp.startEnd(warmUp::begin, warmUp::end));
  }
}
