package frc.robot.challenges.processinputs;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import org.littletonrobotics.junction.Logger;

/** The shooter: it holds a target speed, using the speed it reads from its IO layer. */
public class Shooter extends SubsystemBase {
  private static final double KV = 12.0 / 6000.0;
  private static final double KP = 0.02;

  private final ShooterIO io;
  private final ShooterIOInputsAutoLogged inputs = new ShooterIOInputsAutoLogged();
  private double targetRpm = 0.0;

  public Shooter(ShooterIO io) {
    this.io = io;
  }

  @Override
  public void periodic() {
    // TODO: Ask the IO layer to fill in the inputs.

    // TODO: Send the inputs to AdvantageKit under the name "Shooter".

    double volts = 0.0;
    if (targetRpm != 0.0) {
      volts = MathUtil.clamp(KV * targetRpm + KP * (targetRpm - getRpm()), -12.0, 12.0);
    }
    io.setVoltage(volts);
    Logger.recordOutput("Shooter/CommandedVolts", volts);
    Logger.recordOutput("Shooter/MeasuredRPM", getRpm());
  }

  public void setTargetRpm(double rpm) {
    targetRpm = rpm;
  }

  public void stop() {
    targetRpm = 0.0;
  }

  /** How fast the shooter is spinning. */
  public double getRpm() {
    return Units.radiansPerSecondToRotationsPerMinute(inputs.velocityRadPerSec);
  }
}
