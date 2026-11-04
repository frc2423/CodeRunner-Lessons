package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.flywheel.Flywheel;
import org.littletonrobotics.junction.Logger;

public class Challenge02EveryLoop extends LabChallenge {
  public enum ShooterState {
    IDLE,
    SPINNING_UP,
    READY
  }

  private static class ShooterStatus extends SubsystemBase {
    private final Flywheel flywheel;

    ShooterStatus(Flywheel flywheel) {
      this.flywheel = flywheel;
    }

    @Override
    public void periodic() {
      Logger.recordOutput("Shooter/MeasuredRPM", flywheel.getRpm());
      Logger.recordOutput("Shooter/AtSpeed", flywheel.atSpeed());

      ShooterState state;
      if (flywheel.getTargetRpm() == 0.0) {
        state = ShooterState.IDLE;
      } else if (flywheel.atSpeed()) {
        state = ShooterState.READY;
      } else {
        state = ShooterState.SPINNING_UP;
      }
      Logger.recordOutput("Shooter/State", state);
    }
  }

  private final Flywheel flywheel = Flywheel.create();
  private final ShooterStatus status = new ShooterStatus(flywheel);

  @Override
  public void configureBindings() {
    controller
        .a()
        .whileTrue(flywheel.startEnd(() -> flywheel.setTargetRpm(3000.0), flywheel::stop));
  }
}
