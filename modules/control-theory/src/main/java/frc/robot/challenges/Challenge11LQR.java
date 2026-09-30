package frc.robot.challenges;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.controller.LinearQuadraticRegulator;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.system.LinearSystem;
import edu.wpi.first.math.system.plant.LinearSystemId;
import frc.robot.lab.Controller;
import frc.robot.lab.Mechanism;
import frc.robot.lab.TunableNumber;

/**
 * Challenge 11: LQR.
 *
 * <p>Picking kP by trial and error works, but a model lets us do better. A <b>linear-quadratic
 * regulator</b> (LQR) works out the best gain for you from the mechanism's model and two simple
 * questions: how much error can you live with, and how much voltage are you willing to use?
 *
 * <p>Use the constants you found for the mystery flywheel in challenge 10. WPILib's matrices are
 * made with {@link VecBuilder}: {@code VecBuilder.fill(100.0)} is a 1×1 vector holding 100.
 */
public class Challenge11LQR implements Controller {
  // TODO: Put in the constants you found in the Mystery Flywheel challenge.
  private static final double kS = 0.0;
  private static final double kV = 0.0;
  private static final double kA = 0.0;

  /** "How far from the goal (RPM) is too far?" Smaller makes a more aggressive controller. */
  private final TunableNumber maxError = new TunableNumber("maxError", 100.0);

  /** "How many volts is a lot?" Smaller makes a gentler controller. */
  private final TunableNumber maxVolts = new TunableNumber("maxVolts", 12.0);

  private LinearQuadraticRegulator<N1, N1, N1> lqr;

  @Override
  public void reset(Mechanism flywheel) {
    if (kV == 0.0 || kA == 0.0) {
      throw new IllegalStateException("Put your Mystery Flywheel constants in kS, kV and kA first");
    }
    // Built here rather than in a field, so every Run uses the latest slider values.
    LinearSystem<N1, N1, N1> plant = LinearSystemId.identifyVelocitySystem(kV, kA);
    // TODO: Make the LQR from the plant, the two tolerances, and the loop time.
  }

  @Override
  public double calculate(Mechanism flywheel) {
    // TODO: Feedforward (kS and kV) plus the LQR's feedback, clamped to ±12 volts.
    double volts = 0.0;
    return MathUtil.clamp(volts, -12.0, 12.0);
  }
}
