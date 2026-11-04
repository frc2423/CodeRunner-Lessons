package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.flywheel.Flywheel;
import org.littletonrobotics.junction.AutoLogOutput;

public class Challenge04AutoLogOutput extends LabChallenge {
  public static class ShooterTelemetry extends SubsystemBase {
    private final Flywheel flywheel;

    @AutoLogOutput private int shotsRequested = 0;

    @AutoLogOutput(key = "Shooter/TargetRPM", unit = "rpm")
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

    @AutoLogOutput(key = "Shooter/Ready")
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
