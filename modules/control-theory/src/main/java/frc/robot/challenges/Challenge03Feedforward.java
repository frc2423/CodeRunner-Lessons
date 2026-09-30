package frc.robot.challenges;

import frc.robot.lab.Controller;
import frc.robot.lab.Mechanism;
import frc.robot.lab.TunableNumber;

/**
 * Challenge 3: Feedforward.
 *
 * <p>A P controller only pushes when there's an error, so it always ends up a little short. A
 * <b>feedforward</b> works out the voltage the flywheel needs <i>before</i> anything goes wrong,
 * from what we know about the motor:
 *
 * <pre>
 *   feedforward = kS * sign(goal) + kV * goal     (beat friction, then hold the speed)
 *   feedback    = kP * (goal - velocity)          (fix whatever the feedforward missed)
 *   volts       = feedforward + feedback
 * </pre>
 *
 * <p>Then survive three balls going through the shooter!
 */
public class Challenge03Feedforward implements Controller {
  private final TunableNumber kS = new TunableNumber("kS", 0.0);
  private final TunableNumber kV = new TunableNumber("kV", 0.0);
  private final TunableNumber kP = new TunableNumber("kP", 0.0);

  @Override
  public double calculate(Mechanism flywheel) {
    // TODO: Add a feedforward and a P controller together.
    return 0.0;
  }
}
