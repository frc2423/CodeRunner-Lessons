package frc.robot.challenges.processinputs;

import org.littletonrobotics.junction.AutoLog;

/**
 * The shooter's IO layer. Nothing here knows about any particular motor controller.
 *
 * <p>{@code @AutoLog} makes AdvantageKit generate a class called {@code ShooterIOInputsAutoLogged}
 * when your code builds. It has the same fields, plus the code that saves them to the log and reads
 * them back during a replay. Fields are logged with their first letter capitalized: {@code
 * velocityRadPerSec} is logged as {@code VelocityRadPerSec}.
 */
public interface ShooterIO {
  @AutoLog
  public static class ShooterIOInputs {
    public double velocityRadPerSec = 0.0;
    public double appliedVolts = 0.0;
  }

  /** Reads every sensor once, and copies the values into {@code inputs}. */
  public default void updateInputs(ShooterIOInputs inputs) {}

  /** Runs the motor at this voltage. */
  public default void setVoltage(double volts) {}
}
