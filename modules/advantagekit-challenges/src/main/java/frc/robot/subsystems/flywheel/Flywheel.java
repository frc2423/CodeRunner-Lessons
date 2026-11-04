package frc.robot.subsystems.flywheel;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import org.littletonrobotics.junction.Logger;

/**
 * The shooter's flywheel. Ask it for a speed, and it holds that speed on its own. It's a finished
 * example of the AdvantageKit pattern: try not to read it until you've done the challenges on IO
 * layers!
 */
public class Flywheel extends SubsystemBase {
  private static final double KV = 12.0 / 6000.0;
  private static final double KP = 0.02;
  private static final double AT_SPEED_TOLERANCE_RPM = 100.0;

  private final FlywheelIO io;
  private final FlywheelIOInputsAutoLogged inputs = new FlywheelIOInputsAutoLogged();
  private double targetRpm = 0.0;

  /** A flywheel with the right IO for how the program is running. */
  public static Flywheel create() {
    return new Flywheel(
        switch (Constants.currentMode) {
          case REPLAY -> new FlywheelIO() {};
          default -> new FlywheelIOSim();
        });
  }

  public Flywheel(FlywheelIO io) {
    this.io = io;
  }

  @Override
  public void periodic() {
    io.updateInputs(inputs);
    Logger.processInputs("Flywheel", inputs);

    double volts = 0.0;
    if (targetRpm != 0.0) {
      volts = MathUtil.clamp(KV * targetRpm + KP * (targetRpm - getRpm()), -12.0, 12.0);
    }
    io.setVoltage(volts);
    Logger.recordOutput("Flywheel/CommandedVolts", volts);
  }

  /** Spin at this speed until told otherwise. */
  public void setTargetRpm(double rpm) {
    targetRpm = rpm;
  }

  public void stop() {
    targetRpm = 0.0;
  }

  public double getTargetRpm() {
    return targetRpm;
  }

  /** How fast the flywheel is really spinning. */
  public double getRpm() {
    return Units.radiansPerSecondToRotationsPerMinute(inputs.velocityRadPerSec);
  }

  /** True when the flywheel has a target and is within 100 RPM of it. */
  public boolean atSpeed() {
    return targetRpm != 0.0 && Math.abs(getRpm() - targetRpm) <= AT_SPEED_TOLERANCE_RPM;
  }
}
