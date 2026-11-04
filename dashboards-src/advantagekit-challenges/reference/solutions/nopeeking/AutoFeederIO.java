package frc.robot.challenges.nopeeking;

import org.littletonrobotics.junction.AutoLog;

public interface AutoFeederIO {
  @AutoLog
  public static class AutoFeederIOInputs {
    public double flywheelVelocityRadPerSec = 0.0;
    public double appliedVolts = 0.0;
    public boolean hasPiece = false;
  }

  public default void updateInputs(AutoFeederIOInputs inputs) {}

  public default void setVoltage(double volts) {}
}
