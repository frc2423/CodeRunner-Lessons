package frc.robot.challenges.mystery;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.Logger;

/** The fix: the tolerance had an extra zero, so "ready" was true 1500 RPM short of the target. */
// NAIVE: a smaller tolerance, but it's still too big.
public class Shooter extends SubsystemBase {
  public static final double NEAR_RPM = 2500.0;
  public static final double FAR_RPM = 5000.0;

  private static final double KV = 12.0 / 6000.0;
  private static final double KP = 0.02;
  private static final double READY_TOLERANCE_RPM = 1000.0;
  private static final double FEED_VOLTS = 12.0;

  private final ShooterIO io;
  private final ShooterIOInputsAutoLogged inputs = new ShooterIOInputsAutoLogged();

  @AutoLogOutput(key = "Shooter/TargetRPM")
  private double targetRpm = 0.0;

  @AutoLogOutput(key = "Shooter/Feeding")
  private boolean feeding = false;

  public Shooter(ShooterIO io) {
    this.io = io;
  }

  @Override
  public void periodic() {
    io.updateInputs(inputs);
    Logger.processInputs("Shooter", inputs);

    double rpm = Units.radiansPerSecondToRotationsPerMinute(inputs.flywheelRadPerSec);
    double volts = targetRpm == 0.0 ? 0.0 : KV * targetRpm + KP * (targetRpm - rpm);
    io.setFlywheelVoltage(MathUtil.clamp(volts, -12.0, 12.0));
    io.setFeederVoltage(feeding ? FEED_VOLTS : 0.0);
    Logger.recordOutput("Shooter/ErrorRPM", targetRpm - rpm);
  }

  public void setTargetRpm(double rpm) {
    targetRpm = rpm;
  }

  private boolean ready() {
    double rpm = Units.radiansPerSecondToRotationsPerMinute(inputs.flywheelRadPerSec);
    return targetRpm != 0.0 && Math.abs(targetRpm - rpm) < READY_TOLERANCE_RPM;
  }

  public Command fire() {
    return Commands.sequence(
            Commands.waitUntil(this::ready),
            runOnce(() -> feeding = true),
            Commands.waitUntil(() -> !inputs.hasPiece),
            runOnce(() -> feeding = false))
        .withName("Fire");
  }
}
