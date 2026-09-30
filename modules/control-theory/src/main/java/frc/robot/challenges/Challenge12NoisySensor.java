package frc.robot.challenges;

import edu.wpi.first.math.filter.LinearFilter;
import frc.robot.lab.Controller;
import frc.robot.lab.Mechanism;
import frc.robot.lab.TunableNumber;

/**
 * Challenge 12: Noisy Sensor.
 *
 * <p>Back to the practice flywheel, but its speed sensor is terrible: every reading is off by
 * around 120 RPM, at random. A P controller turns that noise straight into shaky voltage. Smooth
 * the readings with a <b>low-pass filter</b> first, and control with the filtered speed.
 *
 * <p>Filters have a price: the more you smooth, the later the filtered speed notices real changes.
 * Find the balance.
 */
public class Challenge12NoisySensor implements Controller {
  private final TunableNumber kS = new TunableNumber("kS", 0.0);
  private final TunableNumber kV = new TunableNumber("kV", 0.0);
  private final TunableNumber kP = new TunableNumber("kP", 0.0);
  private final TunableNumber timeConstant = new TunableNumber("timeConstant", 0.1);

  private LinearFilter filter;

  @Override
  public void reset(Mechanism flywheel) {
    // Averages away noise faster than about timeConstant seconds.
    filter = LinearFilter.singlePoleIIR(timeConstant.get(), flywheel.dt());
  }

  @Override
  public double calculate(Mechanism flywheel) {
    // TODO: Filter the measured velocity, show it with flywheel.showEstimate(…), and use it in
    // your feedforward + P controller from challenge 3.
    return 0.0;
  }
}
