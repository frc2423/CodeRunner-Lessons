package frc.robot.lab;

/**
 * Every challenge is a class that implements this interface: a controller that decides how many
 * volts to give a motor.
 *
 * <p>When you pick a challenge on the <b>Control Lab</b> dashboard and click <b>Run trial</b>, the
 * robot makes a new object of that challenge's class, calls {@link #reset} once, and then calls
 * {@link #calculate} every 20 milliseconds until the trial ends. Each time, you read the
 * mechanism's sensors and goal, and return a voltage.
 *
 * <p>You don't need to edit this file.
 */
public interface Controller {
  /**
   * Runs once when a trial (or sandbox) starts, before the first {@link #calculate}. Use it to
   * reset anything your controller remembers, or to build objects from the latest tunable values.
   *
   * <p>You don't have to write this method. Leave it out if your controller doesn't need it.
   *
   * @param mechanism the mechanism's sensors and goal at the start
   */
  default void reset(Mechanism mechanism) {}

  /**
   * Runs every 20 milliseconds (50 times a second) while the trial runs.
   *
   * @param mechanism the mechanism's sensors and goal right now
   * @return the voltage to give the motor. The battery can only give -12 to 12 volts, so anything
   *     outside that range is clamped.
   */
  double calculate(Mechanism mechanism);
}
