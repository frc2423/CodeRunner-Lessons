package frc.robot.subsystems.flywheel;

import org.littletonrobotics.junction.AutoLog;

/** The flywheel's hardware layer: everything the {@link Flywheel} subsystem reads and commands. */
public interface FlywheelIO {
  @AutoLog
  public static class FlywheelIOInputs {
    public double velocityRadPerSec = 0.0;
    public double appliedVolts = 0.0;
    public double currentAmps = 0.0;
  }

  /** Updates the set of loggable inputs. */
  public default void updateInputs(FlywheelIOInputs inputs) {}

  /** Runs the motor at this voltage. */
  public default void setVoltage(double volts) {}
}
