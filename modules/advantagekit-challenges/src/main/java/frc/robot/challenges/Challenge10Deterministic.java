package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.flywheel.Flywheel;
import org.littletonrobotics.junction.Logger;

/**
 * Challenge 10: Same Answer Every Time.
 *
 * <p>Holding A runs a warm-up routine: the flywheel's target climbs from 1000 RPM to 3000 RPM over
 * two seconds, with a small random wobble so it doesn't sit on one resonant speed.
 *
 * <p>This code never touches the hardware, and its replay still doesn't match. Replay gives your
 * code the same inputs, so anything that gives a different answer each time breaks it. Find the two
 * places in {@link WarmUp} that do, and fix them. The wobble has to stay.
 */
public class Challenge10Deterministic extends LabChallenge {
  private static class WarmUp extends SubsystemBase {
    private final Flywheel flywheel;
    private boolean running = false;
    private long startMillis = 0;

    WarmUp(Flywheel flywheel) {
      this.flywheel = flywheel;
    }

    void begin() {
      running = true;
      startMillis = System.currentTimeMillis();
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
      double elapsed = (System.currentTimeMillis() - startMillis) / 1000.0;
      double base = 1000.0 + Math.min(elapsed, 2.0) * 1000.0;
      double wobble = (Math.random() - 0.5) * 100.0;

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
