package frc.robot.challenges;

import frc.robot.lab.Controller;
import frc.robot.lab.Mechanism;

/**
 * Challenge 10: Mystery Flywheel (System Identification).
 *
 * <p>This flywheel is a mystery: nobody knows its kS, kV or kA. <b>System identification</b> finds
 * them from data, the way WPILib's SysId tool does. Your job is to write the <i>test</i>: the
 * voltages to apply during the 8 seconds of the trial. The lab records how the flywheel responds
 * and fits the model {@code volts = kS + kV * velocity + kA * acceleration} to it.
 *
 * <p>A good test shows the flywheel at many different speeds (a slow <b>quasistatic</b> ramp) and
 * accelerating hard (a sudden <b>dynamic</b> step). Write down the constants the lab finds: you'll
 * need them in the next challenges.
 */
public class Challenge10SysId implements Controller {
  @Override
  public double calculate(Mechanism flywheel) {
    double t = flywheel.time();
    // TODO: Return test voltages that depend on the time t (0 to 8 seconds).
    return 0.0;
  }
}
