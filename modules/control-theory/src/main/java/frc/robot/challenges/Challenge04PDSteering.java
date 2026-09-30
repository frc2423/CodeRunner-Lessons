package frc.robot.challenges;

import frc.robot.lab.Controller;
import frc.robot.lab.Mechanism;
import frc.robot.lab.TunableNumber;

/**
 * Challenge 4: PD Steering.
 *
 * <p>Now control a position: the angle of a swerve module's wheel, in degrees. A P controller acts
 * like a spring pulling the wheel to its goal, and like a spring, it bounces. The <b>derivative</b>
 * (D) term acts like a shock absorber: it pushes back when the error changes fast.
 *
 * <pre>
 *   error      = goal - position
 *   derivative = (error - lastError) / dt
 *   volts      = kP * error + kD * derivative
 * </pre>
 *
 * <p>Write it yourself this time. {@code lastError} has to be remembered from one loop to the next,
 * so it must be a field.
 */
public class Challenge04PDSteering implements Controller {
  private final TunableNumber kP = new TunableNumber("kP", 0.0);
  private final TunableNumber kD = new TunableNumber("kD", 0.0);

  // TODO: Add a field to remember last loop's error.

  @Override
  public void reset(Mechanism wheel) {
    // TODO: Start lastError at the error there is right now, so the first derivative isn't huge.
  }

  @Override
  public double calculate(Mechanism wheel) {
    // TODO: P plus D. Don't forget to remember this loop's error for next time.
    return 0.0;
  }
}
