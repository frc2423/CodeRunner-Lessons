package frc.robot.challenges;

import frc.robot.lab.Controller;
import frc.robot.lab.Mechanism;

/**
 * Example: Open Loop. This one is already finished, so you can see how a challenge works.
 *
 * <p>It gives the flywheel a fixed 6 volts, without ever looking at its speed. That's called
 * <b>open-loop</b> control. Pick <b>Example: Open Loop</b> on the Control Lab dashboard and click
 * <b>Run trial</b>. The flywheel spins up to about 2900 RPM: close to the 3000 RPM goal, but not
 * quite, and nothing in this code can notice or fix that.
 */
public class Challenge00OpenLoop implements Controller {
  @Override
  public double calculate(Mechanism flywheel) {
    // The motor gets 6 volts, no matter what. Try 12 (full power) or 3.
    return 6.0;
  }
}
