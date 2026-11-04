package frc.robot.subsystems.feeder;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import org.littletonrobotics.junction.Logger;

/**
 * The belts that push a game piece into the flywheel, with a beam break that sees the piece. It's a
 * finished example of the AdvantageKit pattern: try not to read it until you've done the challenges
 * on IO layers!
 */
public class Feeder extends SubsystemBase {
  private static final double FEED_VOLTS = 6.0;

  private final FeederIO io;
  private final FeederIOInputsAutoLogged inputs = new FeederIOInputsAutoLogged();
  private double volts = 0.0;

  /** A feeder with the right IO for how the program is running. */
  public static Feeder create() {
    return new Feeder(
        switch (Constants.currentMode) {
          case REPLAY -> new FeederIO() {};
          default -> new FeederIOSim();
        });
  }

  public Feeder(FeederIO io) {
    this.io = io;
  }

  @Override
  public void periodic() {
    io.updateInputs(inputs);
    Logger.processInputs("Feeder", inputs);
    io.setVoltage(volts);
    Logger.recordOutput("Feeder/CommandedVolts", volts);
  }

  public void run() {
    volts = FEED_VOLTS;
  }

  public void stop() {
    volts = 0.0;
  }

  /** The beam break: true while a game piece is in the feeder. */
  public boolean hasPiece() {
    return inputs.hasPiece;
  }

  /** Runs the belts until this command is cancelled or ended, then stops them. */
  public Command feed() {
    return startEnd(this::run, this::stop);
  }
}
