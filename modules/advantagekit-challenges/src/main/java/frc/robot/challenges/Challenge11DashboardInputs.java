package frc.robot.challenges;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.flywheel.Flywheel;
import org.littletonrobotics.junction.Logger;

/**
 * Challenge 11: Dashboard Inputs.
 *
 * <p>Holding A spins the flywheel up to a speed the drive team picks on the dashboard. Something
 * typed on a dashboard is an input too, and this code reads it where AdvantageKit can't see it. In
 * a replay there's no dashboard, so the robot falls back to the default speed.
 *
 * <p>Replace the {@code SmartDashboard} read with a {@code LoggedNetworkNumber} at {@code
 * /Tuning/TargetRPM}, so the value is logged and replayed.
 */
public class Challenge11DashboardInputs extends LabChallenge {
  private static class TunedShooter extends SubsystemBase {
    private final Flywheel flywheel;
    private boolean running = false;

    // TODO: Add a LoggedNetworkNumber field at "/Tuning/TargetRPM", with a default of 3000.0.

    // TODO (bonus): Add a LoggedDashboardChooser<Double> field called "ShotPreset". In the
    // constructor, give it a default option "Near" = 0.0 and an option "Far" = 1000.0. Add its
    // value to the speed, and log the total as usual.

    TunedShooter(Flywheel flywheel) {
      this.flywheel = flywheel;
      SmartDashboard.putNumber("TargetRPM", 3000.0);
    }

    void setRunning(boolean running) {
      this.running = running;
      if (!running) {
        flywheel.stop();
      }
    }

    @Override
    public void periodic() {
      // TODO: Read the target speed from your LoggedNetworkNumber instead.
      double target = SmartDashboard.getNumber("TargetRPM", 3000.0);

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
