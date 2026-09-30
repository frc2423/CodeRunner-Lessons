package frc.robot.challenges;

import edu.wpi.first.math.controller.PIDController;
import frc.robot.lab.Controller;
import frc.robot.lab.Mechanism;
import frc.robot.lab.TunableNumber;

/**
 * Challenge 8: Arm Feedforward.
 *
 * <p>An arm on a pivot, with its angle in <b>radians</b>: 0 is sticking straight out, π/2 is
 * pointing straight up. Gravity pulls hardest when the arm is horizontal and not at all when it
 * points straight up, so the gravity feedforward has to change with the angle:
 *
 * <pre>
 *   gravity feedforward = kG * cos(angle)
 * </pre>
 */
public class Challenge08ArmFeedforward implements Controller {
  private final TunableNumber kP = new TunableNumber("kP", 0.0);
  private final TunableNumber kD = new TunableNumber("kD", 0.0);
  private final TunableNumber kG = new TunableNumber("kG", 0.0);

  private final PIDController pid = new PIDController(0.0, 0.0, 0.0);

  @Override
  public double calculate(Mechanism arm) {
    pid.setPID(kP.get(), 0.0, kD.get());
    // TODO: PID output plus a gravity feedforward that depends on the arm's angle.
    return 0.0;
  }
}
