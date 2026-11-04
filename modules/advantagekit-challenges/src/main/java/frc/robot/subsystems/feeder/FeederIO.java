package frc.robot.subsystems.feeder;

import org.littletonrobotics.junction.AutoLog;

/** The feeder's hardware layer: everything the {@link Feeder} subsystem reads and commands. */
public interface FeederIO {
  @AutoLog
  public static class FeederIOInputs {
    public boolean hasPiece = false;
    public double appliedVolts = 0.0;
    public double currentAmps = 0.0;
  }

  /** Updates the set of loggable inputs. */
  public default void updateInputs(FeederIOInputs inputs) {}

  /** Runs the motor at this voltage. */
  public default void setVoltage(double volts) {}
}
