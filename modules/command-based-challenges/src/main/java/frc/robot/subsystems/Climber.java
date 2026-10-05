package frc.robot.subsystems;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

/**
 * The climber: a hook on a tube that reaches up to the tower's bar, then pulls the robot up off the
 * floor. You write this subsystem in challenge 11.
 *
 * <p>A subsystem owns its hardware, so this class makes the {@link ClimberMotor}, and nothing else
 * in the robot should touch it. Other code uses the climber through the commands this class makes.
 * Because they're made with the subsystem's own {@code run}, {@code runEnd}, etc., they all require
 * the climber, so only one of them can run at a time.
 */
public class Climber extends SubsystemBase {
  /** Stop going up here: a little below the hard stop at {@link ClimberMotor#MAX_HEIGHT}. */
  public static final double TOP = 0.58;

  /** Volts for going up. */
  public static final double EXTEND_VOLTS = 8.0;

  /** Volts for pulling the robot up (the hook goes down). */
  public static final double CLIMB_VOLTS = -10.0;

  private final ClimberMotor motor = new ClimberMotor();

  @Override
  public void periodic() {
    // TODO (optional): Log the climber's height with Logger.recordOutput("Climber/Height", ...).
  }

  /** Raises the hook until it reaches {@link #TOP}, then stops the motor. */
  public Command extend() {
    // TODO
    return Commands.none();
  }

  /** Pulls the hook down until the bottom limit switch is pressed, then stops the motor. */
  public Command climb() {
    // TODO
    return Commands.none();
  }
}
