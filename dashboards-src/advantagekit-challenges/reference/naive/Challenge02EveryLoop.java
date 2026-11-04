package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.flywheel.Flywheel;
import org.littletonrobotics.junction.Logger;

// NAIVE: logs the speed once when the button goes down, so the log never hears about it again.
public class Challenge02EveryLoop extends LabChallenge {
  public enum ShooterState {
    IDLE,
    SPINNING_UP,
    READY
  }

  private static class ShooterStatus extends SubsystemBase {}

  private final Flywheel flywheel = Flywheel.create();
  private final ShooterStatus status = new ShooterStatus();

  @Override
  public void configureBindings() {
    controller
        .a()
        .whileTrue(
            flywheel.startEnd(
                () -> {
                  flywheel.setTargetRpm(3000.0);
                  Logger.recordOutput("Shooter/MeasuredRPM", flywheel.getRpm());
                  Logger.recordOutput("Shooter/AtSpeed", flywheel.atSpeed());
                  Logger.recordOutput("Shooter/State", ShooterState.SPINNING_UP);
                },
                flywheel::stop));
  }
}
