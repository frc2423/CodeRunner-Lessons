package frc.robot.subsystems;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.Hardware;
import org.littletonrobotics.junction.Logger;

/**
 * The indexer: a set of belts between the intake and the shooter. It pulls a piece in from the
 * intake and holds it at the beam break sensor, then feeds it into the shooter when it's time to
 * shoot.
 *
 * <p>Careful: if the indexer keeps running once a piece reaches the beam break, it pushes the piece
 * on into the shooter. If the shooter isn't spinning, the piece just dribbles out.
 */
public class Indexer extends SubsystemBase {
  /** Belt speed for pulling a piece in from the intake. */
  public static final double LOAD_SPEED = 1.0;

  /** Belt speed for feeding a piece into the shooter. */
  public static final double FEED_SPEED = 1.0;

  @Override
  public void periodic() {
    Logger.recordOutput("Indexer/Speed", Hardware.indexer());
    Logger.recordOutput("Indexer/HasPiece", hasPiece());
  }

  /** Runs the belts: positive moves pieces toward the shooter, negative back toward the intake. */
  public void setSpeed(double speed) {
    Hardware.setIndexer(speed);
  }

  /** Stops the belts. */
  public void stop() {
    Hardware.setIndexer(0);
  }

  /** The beam break sensor: true while a piece is sitting in front of it, ready to shoot. */
  public boolean hasPiece() {
    return Hardware.beamBreak();
  }
}
