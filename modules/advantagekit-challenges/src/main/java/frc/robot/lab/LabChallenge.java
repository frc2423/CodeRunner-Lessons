package frc.robot.lab;

/**
 * What every challenge builds on. A challenge is a small RobotContainer: it makes its subsystems,
 * and binds commands to the driver's controller in {@link #configureBindings()}.
 *
 * <p>The lab makes a new challenge object every time you click <b>Run trial</b> or <b>Free
 * play</b>, after clearing every old binding, default command and logged value. A replay does the
 * same with the logged inputs, so the code in your challenge runs exactly the same way both times.
 * You don't need to edit this file.
 */
public abstract class LabChallenge {
  /** The driver's Xbox controller (or keyboard, or the dashboard's buttons). */
  protected final LabController controller = AKitLab.controller();

  /** Called once when the challenge starts: bind commands to buttons and set default commands. */
  public abstract void configureBindings();
}
