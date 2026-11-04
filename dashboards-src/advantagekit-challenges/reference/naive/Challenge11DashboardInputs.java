package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.flywheel.Flywheel;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.networktables.LoggedDashboardChooser;
import org.littletonrobotics.junction.networktables.LoggedNetworkNumber;

// NAIVE: makes the logged number, but still reads SmartDashboard.
public class Challenge11DashboardInputs extends LabChallenge {
  private static class TunedShooter extends SubsystemBase {
    private final Flywheel flywheel;
    private boolean running = false;
    private final LoggedNetworkNumber targetRpm =
        new LoggedNetworkNumber("/Tuning/TargetRPM", 3000.0);
    private final LoggedDashboardChooser<Double> preset = new LoggedDashboardChooser<>("ShotPreset");

    TunedShooter(Flywheel flywheel) {
      this.flywheel = flywheel;
      preset.addDefaultOption("Near", 0.0);
      preset.addOption("Far", 1000.0);
    }

    void setRunning(boolean running) {
      this.running = running;
      if (!running) {
        flywheel.stop();
      }
    }

    @Override
    public void periodic() {
      Double extra = preset.get();
      double target =
          edu.wpi.first.wpilibj.smartdashboard.SmartDashboard.getNumber("TargetRPM", 3000.0)
              + (extra == null ? 0.0 : extra);

      if (running) {
        flywheel.setTargetRpm(target);
      }
      Logger.recordOutput("TunedShooter/TargetRPM", target);
    }
  }

  private final Flywheel flywheel = Flywheel.create();
  private final TunedShooter shooter = new TunedShooter(flywheel);

  @Override
  public void configureBindings() {
    controller
        .a()
        .whileTrue(
            shooter.startEnd(() -> shooter.setRunning(true), () -> shooter.setRunning(false)));
  }
}
