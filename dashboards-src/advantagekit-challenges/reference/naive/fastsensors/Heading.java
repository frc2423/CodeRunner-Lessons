package frc.robot.challenges.fastsensors;

import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import org.littletonrobotics.junction.Logger;

// NAIVE: fills in the arrays, and only uses the newest sample.
public class Heading extends SubsystemBase {
  private static final double SAMPLE_PERIOD = 0.004;

  private final GyroIO io;
  private final GyroIOInputsAutoLogged inputs = new GyroIOInputsAutoLogged();
  private double yawRad = 0.0;

  public Heading(GyroIO io) {
    this.io = io;
  }

  @Override
  public void periodic() {
    io.updateInputs(inputs);
    Logger.processInputs("Gyro", inputs);

    if (inputs.yawRateRadPerSec.length > 0) {
      yawRad += inputs.yawRateRadPerSec[inputs.yawRateRadPerSec.length - 1] * 0.02;
    }

    Logger.recordOutput("Heading/YawDegrees", Units.radiansToDegrees(yawRad));
  }
}
