package frc.robot.challenges;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.estimator.KalmanFilter;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.system.LinearSystem;
import edu.wpi.first.math.system.plant.LinearSystemId;
import frc.robot.lab.Controller;
import frc.robot.lab.Mechanism;
import frc.robot.lab.TunableNumber;

/**
 * Challenge 13: Kalman Filter.
 *
 * <p>The mystery flywheel, now with the terrible sensor. A low-pass filter only looks at the
 * readings. A <b>Kalman filter</b> also knows the model: from the voltage you applied, it
 * <i>predicts</i> the new speed, then <i>corrects</i> the prediction a little toward each reading.
 * It trusts whichever is more reliable, so it can be smooth without lagging behind.
 *
 * <p>You'll need {@link VecBuilder} for the standard deviations, and {@link Nat} to tell the filter
 * the sizes: {@code Nat.N1()} is 1.
 */
public class Challenge13KalmanFilter implements Controller {
  // TODO: Put in the constants you found in the Mystery Flywheel challenge.
  private static final double kS = 0.0;
  private static final double kV = 0.0;
  private static final double kA = 0.0;

  /** How far the model might drift from the truth (RPM). */
  private final TunableNumber modelStdDev = new TunableNumber("modelStdDev", 50.0);

  /** How noisy the sensor is (RPM). */
  private final TunableNumber sensorStdDev = new TunableNumber("sensorStdDev", 120.0);

  private final TunableNumber kP = new TunableNumber("kP", 0.0);

  private KalmanFilter<N1, N1, N1> observer;

  @Override
  public void reset(Mechanism flywheel) {
    if (kV == 0.0 || kA == 0.0) {
      throw new IllegalStateException("Put your Mystery Flywheel constants in kS, kV and kA first");
    }
    LinearSystem<N1, N1, N1> plant = LinearSystemId.identifyVelocitySystem(kV, kA);
    // TODO: Make the KalmanFilter from the plant, the two standard deviations and the loop time.
  }

  @Override
  public double calculate(Mechanism flywheel) {
    // TODO: 1. Predict with the voltage the motor got last loop (minus what friction used up).
    //       2. Correct with this loop's measured velocity.
    //       3. Show the estimate, and control with it: feedforward + kP * error.
    double volts = 0.0;
    return MathUtil.clamp(volts, -12.0, 12.0);
  }
}
