package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.flywheel.Flywheel;

/**
 * Challenge 4: Let the Annotation Do It.
 *
 * <p>Writing {@code Logger.recordOutput} for every value gets old. With {@code @AutoLogOutput} on a
 * field or a method, AdvantageKit logs it for you, once per loop, even if it's private.
 *
 * <p>Don't write {@code Logger.recordOutput} in this challenge. Annotate the three members of
 * {@link ShooterTelemetry} marked TODO instead.
 */
public class Challenge04AutoLogOutput extends LabChallenge {
  /** Counts shots and works out whether the shooter's ready. */
  public static class ShooterTelemetry extends SubsystemBase {
    private final Flywheel flywheel;

    // TODO: Annotate this so it's logged with its automatic key.
    private int shotsRequested = 0;

    // TODO: Annotate this so it's logged at the key "Shooter/TargetRPM", with the unit "rpm".
    private double targetRpm = 3000.0;

    public ShooterTelemetry(Flywheel flywheel) {
      this.flywheel = flywheel;
    }

    public void countShot() {
      shotsRequested++;
    }

    public double getTargetRpm() {
      return targetRpm;
    }

    // TODO: Annotate this so its answer is logged at the key "Shooter/Ready".
    public boolean isReady() {
      return flywheel.atSpeed();
    }
  }

  private final Flywheel flywheel = Flywheel.create();
  private final ShooterTelemetry telemetry = new ShooterTelemetry(flywheel);

  @Override
  public void configureBindings() {
    controller
        .a()
        .whileTrue(
            flywheel.startEnd(
                () -> flywheel.setTargetRpm(telemetry.getTargetRpm()), flywheel::stop));
    controller.x().onTrue(telemetry.runOnce(telemetry::countShot));
  }
}
