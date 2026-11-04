package frc.robot.challenges.fromscratch;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.Logger;

public class Pivot extends SubsystemBase {
  private static final double KP = 12.0;

  private final PivotIO io;
  private final PivotIOInputsAutoLogged inputs = new PivotIOInputsAutoLogged();
  private double setpointDegrees = 0.0;

  public Pivot(PivotIO io) {
    this.io = io;
  }

  @Override
  public void periodic() {
    io.updateInputs(inputs);
    Logger.processInputs("Pivot", inputs);

    double errorRad = Units.degreesToRadians(setpointDegrees) - inputs.positionRad;
    io.setVoltage(MathUtil.clamp(KP * errorRad, -12.0, 12.0));
    Logger.recordOutput("Pivot/SetpointDegrees", setpointDegrees);
  }

  public void setSetpointDegrees(double degrees) {
    setpointDegrees = degrees;
  }

  @AutoLogOutput(key = "Pivot/AtSetpoint")
  public boolean atSetpoint() {
    return Math.abs(Units.radiansToDegrees(inputs.positionRad) - setpointDegrees) < 3.0;
  }
}
