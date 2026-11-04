package frc.robot.challenges.nopeeking;

import org.littletonrobotics.junction.AutoLog;

/** The auto feeder's IO layer. */
public interface AutoFeederIO {
  @AutoLog
  public static class AutoFeederIOInputs {
    public double flywheelVelocityRadPerSec = 0.0;
    public double appliedVolts = 0.0;
  }

  /** Updates the set of loggable inputs. */
  public default void updateInputs(AutoFeederIOInputs inputs) {}

  /** Runs the motor at this voltage. */
  public default void setVoltage(double volts) {}
}
