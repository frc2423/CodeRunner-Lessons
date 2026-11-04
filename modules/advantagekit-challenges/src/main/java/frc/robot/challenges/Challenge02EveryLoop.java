package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.flywheel.Flywheel;

/**
 * Challenge 2: Log Every Loop.
 *
 * <p>Holding A spins the flywheel up. A value in the log only changes when your code records it, so
 * a value that changes all the time has to be recorded all the time. A subsystem's {@code
 * periodic()} runs once per robot loop, which makes it the natural place.
 *
 * <p>Fill in {@link ShooterStatus#periodic()} so that the log shows what the shooter is doing,
 * using three different kinds of value.
 */
public class Challenge02EveryLoop extends LabChallenge {
  /** What the shooter is doing right now. */
  public enum ShooterState {
    IDLE,
    SPINNING_UP,
    READY
  }

  /** Watches the flywheel and writes down what it's doing. */
  private static class ShooterStatus extends SubsystemBase {
    private final Flywheel flywheel;

    ShooterStatus(Flywheel flywheel) {
      this.flywheel = flywheel;
    }

    @Override
    public void periodic() {
      // TODO: Record flywheel.getRpm() at the key "Shooter/MeasuredRPM".

      // TODO: Record flywheel.atSpeed() at the key "Shooter/AtSpeed".

      // TODO: Work out the ShooterState (IDLE when flywheel.getTargetRpm() is 0, READY when
      // flywheel.atSpeed(), otherwise SPINNING_UP) and record it at the key "Shooter/State".
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
