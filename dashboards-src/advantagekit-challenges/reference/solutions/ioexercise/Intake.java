package frc.robot.challenges.ioexercise;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import org.littletonrobotics.junction.Logger;

public class Intake extends SubsystemBase {
  private final IntakeIO io;
  private final IntakeIOInputsAutoLogged inputs = new IntakeIOInputsAutoLogged();
  private double volts = 0.0;

  public Intake(IntakeIO io) {
    this.io = io;
  }

  @Override
  public void periodic() {
    io.updateInputs(inputs);
    Logger.processInputs("Intake", inputs);
    io.setVoltage(volts);
    Logger.recordOutput("Intake/CommandedVolts", volts);
  }

  public void setVolts(double volts) {
    this.volts = volts;
  }
}
