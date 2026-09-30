package frc.robot.challenges;

import frc.robot.lab.Controller;
import frc.robot.lab.Mechanism;
import frc.robot.lab.TunableNumber;

/**
 * Challenge 2: Proportional Control.
 *
 * <p>Bang-bang slams between all and nothing. A <b>proportional</b> (P) controller pushes harder
 * the further the flywheel is from its goal:
 *
 * <pre>
 *   error = goal - velocity
 *   volts = kP * error
 * </pre>
 *
 * <p>{@code kP} is a <b>gain</b>. It has a slider on the dashboard, so you can tune it while the
 * robot runs.
 */
public class Challenge02Proportional implements Controller {
  private final TunableNumber kP = new TunableNumber("kP", 0.0);

  @Override
  public double calculate(Mechanism flywheel) {
    // TODO: Work out the error, then return kP.get() times the error.
    return 0.0;
  }
}
