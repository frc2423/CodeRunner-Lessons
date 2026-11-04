package frc.robot.challenges.nopeeking;

import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.Hardware;
import org.littletonrobotics.junction.Logger;

/** Fires a piece as soon as there's one to fire and the flywheel is fast enough. */
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

    boolean hasPiece = Hardware.feeder.hasPiece();
    double flywheelRpm =
        Units.radiansPerSecondToRotationsPerMinute(Hardware.flywheel.getVelocityRadPerSec());
    boolean ready = flywheelRpm >= READY_RPM;
    boolean feeding = wantsToFeed && hasPiece && ready;

    io.setVoltage(feeding ? 6.0 : 0.0);

    Logger.recordOutput("AutoFeeder/HasPiece", hasPiece);
    Logger.recordOutput("AutoFeeder/FlywheelRPM", flywheelRpm);
    Logger.recordOutput("AutoFeeder/Ready", ready);
    Logger.recordOutput("AutoFeeder/Feeding", feeding);
  }

  /** While this command runs, feed whenever there's a piece and the flywheel's ready. */
  public Command feedWhenReady() {
    return startEnd(() -> wantsToFeed = true, () -> wantsToFeed = false);
  }
}
