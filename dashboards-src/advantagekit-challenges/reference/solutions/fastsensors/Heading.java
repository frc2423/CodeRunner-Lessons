package frc.robot.challenges.fastsensors;

import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import org.littletonrobotics.junction.Logger;

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

    for (double rate : inputs.yawRateRadPerSec) {
      yawRad += rate * SAMPLE_PERIOD;
    }

    Logger.recordOutput("Heading/YawDegrees", Units.radiansToDegrees(yawRad));
  }
}
