package frc.robot.challenges;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.controller.LinearQuadraticRegulator;
import edu.wpi.first.math.estimator.KalmanFilter;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N2;
import edu.wpi.first.math.system.LinearSystem;
import edu.wpi.first.math.system.LinearSystemLoop;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import frc.robot.lab.Controller;
import frc.robot.lab.Mechanism;
import frc.robot.lab.TunableNumber;

/**
 * Challenge 14: State-Space Elevator.
 *
 * <p>Put it all together, the way the book's elevator example does. The elevator's state is two
 * numbers, its height and its velocity, but the only sensor measures height, and it's noisy.
 *
 * <ul>
 *   <li>A <b>motion profile</b> ({@link TrapezoidProfile}) plans where the elevator should be each
 *       loop.
 *   <li>A <b>Kalman filter</b> ({@link KalmanFilter}) estimates both height and velocity from the
 *       noisy height.
 *   <li>An <b>LQR</b> ({@link LinearQuadraticRegulator}) pushes the estimated state toward the
 *       profile's.
 *   <li>A <b>feedforward</b> holds the carriage up against gravity and friction.
 * </ul>
 *
 * <p>WPILib's {@link LinearSystemLoop} runs the LQR and the Kalman filter together for you. You'll
 * also need {@link VecBuilder} and {@link Nat}.
 *
 * <p>The elevator's constants: kS = 0.15 V, kG = 0.45 V, kV = 6.0 V/(m/s), kA = 0.6 V/(m/s²).
 */
public class Challenge14StateSpaceElevator implements Controller {
  private static final double kS = 0.15;
  private static final double kG = 0.45;
  private static final double kV = 6.0;
  private static final double kA = 0.6;

  private final TunableNumber maxHeightError = new TunableNumber("maxHeightError", 0.02);
  private final TunableNumber maxVelocityError = new TunableNumber("maxVelocityError", 0.4);
  private final TunableNumber maxVelocity = new TunableNumber("maxVelocity", 1.2);
  private final TunableNumber maxAcceleration = new TunableNumber("maxAcceleration", 4.0);

  private LinearSystemLoop<N2, N1, N1> loop;
  private TrapezoidProfile profile;
  private TrapezoidProfile.State setpoint;

  @Override
  @SuppressWarnings("unchecked")
  public void reset(Mechanism elevator) {
    // States: [height, velocity]. Input: [volts]. Output: [height], the only thing we measure
    // (slice(0) keeps just the first output).
    LinearSystem<N2, N1, N1> plant =
        (LinearSystem<N2, N1, N1>) LinearSystemId.identifyPositionSystem(kV, kA).slice(0);
    // TODO: Build a LinearQuadraticRegulator, a KalmanFilter, and a LinearSystemLoop from them.
    // TODO: Make the profile, and start the setpoint and the loop at the elevator's height.
  }

  @Override
  public double calculate(Mechanism elevator) {
    // TODO: 1. Step the profile and give its setpoint to the loop as the next reference.
    //       2. Correct the loop with the measured height, and show its estimated height.
    //       3. Predict (this works out the voltage), and return the loop's voltage plus kG and kS.
    double volts = 0.0;
    return MathUtil.clamp(volts, -12.0, 12.0);
  }
}
