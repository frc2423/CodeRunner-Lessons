package frc.robot.challenges.nopeeking;

import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import org.littletonrobotics.junction.Logger;

// NAIVE: fixes the flywheel read, but still peeks at the beam break.
public class AutoFeeder extends SubsystemBase {
  private static final double READY_RPM = 2900.0;

  private final AutoFeederIO io;
  private final AutoFeederIOInputsAutoLogged inputs = new AutoFeederIOInputsAutoLogged();
  private boolean wantsToFeed = false;

  public AutoFeeder(AutoFeederIO io) {
    this.io = io;
  }

  @Override
  public void periodic() {
    io.updateInputs(inputs);
    Logger.processInputs("AutoFeeder", inputs);

    boolean hasPiece = frc.robot.lab.Hardware.feeder.hasPiece();
    double flywheelRpm =
        Units.radiansPerSecondToRotationsPerMinute(inputs.flywheelVelocityRadPerSec);
    boolean ready = flywheelRpm >= READY_RPM;
    boolean feeding = wantsToFeed && hasPiece && ready;

    io.setVoltage(feeding ? 6.0 : 0.0);

    Logger.recordOutput("AutoFeeder/HasPiece", hasPiece);
    Logger.recordOutput("AutoFeeder/FlywheelRPM", flywheelRpm);
    Logger.recordOutput("AutoFeeder/Ready", ready);
    Logger.recordOutput("AutoFeeder/Feeding", feeding);
  }

  public Command feedWhenReady() {
    return startEnd(() -> wantsToFeed = true, () -> wantsToFeed = false);
  }
}
