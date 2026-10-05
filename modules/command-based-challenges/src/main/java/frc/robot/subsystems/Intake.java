package frc.robot.subsystems;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.Hardware;
import org.littletonrobotics.junction.Logger;

/**
 * The intake at the front of the robot. It swings out (deploys) over the bumper, and its rollers
 * grab game pieces off the floor and pass them back to the indexer.
 *
 * <p>It only picks up a piece when it is deployed, its rollers are spinning inward, and the piece
 * is right in front of the robot. The rollers can push a piece into the robot, but the indexer has
 * to pull it the rest of the way to the beam break.
 */
public class Intake extends SubsystemBase {
  /** Roller speed for picking up game pieces. */
  public static final double INTAKE_SPEED = 1.0;

  /** Roller speed for spitting a piece back out (or clearing a jam). */
  public static final double EJECT_SPEED = -1.0;

  @Override
  public void periodic() {
    Logger.recordOutput("Intake/Position", Hardware.intakePosition());
    Logger.recordOutput("Intake/Deployed", isDeployed());
    Logger.recordOutput("Intake/Rollers", Hardware.intakeRollers());
    Logger.recordOutput("Intake/RollerCurrent", getRollerCurrent());
  }

  /** Swings the intake out. It takes about a quarter of a second. */
  public void deploy() {
    Hardware.setIntakeDeployed(true);
  }

  /** Swings the intake back inside the robot. */
  public void retract() {
    Hardware.setIntakeDeployed(false);
  }

  /** Whether the intake is all the way out. */
  public boolean isDeployed() {
    return Hardware.intakePosition() >= 0.9;
  }

  /** Spins the rollers: 1 pulls pieces in at full speed, -1 pushes them out. */
  public void setRollers(double speed) {
    Hardware.setIntakeRollers(speed);
  }

  /** Stops the rollers. */
  public void stopRollers() {
    Hardware.setIntakeRollers(0);
  }

  /**
   * How hard the roller motor is working, in amps. It jumps for a moment whenever a piece comes in,
   * and stays high if a piece gets jammed.
   */
  public double getRollerCurrent() {
    return Hardware.intakeRollerCurrent();
  }
}
