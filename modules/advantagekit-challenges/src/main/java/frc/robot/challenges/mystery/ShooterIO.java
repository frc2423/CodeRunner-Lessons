package frc.robot.challenges.mystery;

import org.littletonrobotics.junction.AutoLog;

/** The shooter's IO layer. */
public interface ShooterIO {
  @AutoLog
  public static class ShooterIOInputs {
    public double flywheelRadPerSec = 0.0;
    public double flywheelVolts = 0.0;
    public double feederVolts = 0.0;
    public boolean hasPiece = false;
  }

  /** Updates the set of loggable inputs. */
  public default void updateInputs(ShooterIOInputs inputs) {}

  public default void setFlywheelVoltage(double volts) {}

  public default void setFeederVoltage(double volts) {}
}
