package frc.robot.challenges;

import frc.robot.lab.Controller;
import frc.robot.lab.Mechanism;

/**
 * Challenge 1: Bang-Bang.
 *
 * <p>Your first <b>closed-loop</b> controller: it measures the flywheel's speed and reacts.
 *
 * <ul>
 *   <li>If the flywheel is slower than the goal, give it full power (12 volts).
 *   <li>Otherwise, give it nothing (0 volts) and let it coast.
 * </ul>
 *
 * <p>{@code flywheel.velocity()} is the measured speed and {@code flywheel.goal()} is the speed the
 * trial wants, both in RPM.
 */
public class Challenge01BangBang implements Controller {
  @Override
  public double calculate(Mechanism flywheel) {
    // TODO: Return 12.0 when the flywheel is too slow, and 0.0 otherwise.
    return 0.0;
  }
}
