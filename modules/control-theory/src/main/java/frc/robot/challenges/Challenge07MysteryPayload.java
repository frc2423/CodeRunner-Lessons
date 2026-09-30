package frc.robot.challenges;

import edu.wpi.first.math.controller.PIDController;
import frc.robot.lab.Controller;
import frc.robot.lab.Mechanism;
import frc.robot.lab.TunableNumber;

/**
 * Challenge 7: Mystery Payload.
 *
 * <p>Two seconds into the trial, the elevator grabs a game piece of unknown weight. Your kG no
 * longer matches, so the carriage sags. The <b>integral</b> (I) term fixes errors that last: it
 * adds up the error over time, and pushes harder the longer the error stays.
 *
 * <p>But careful: during a big move the error is large for a long time. The integral piles up
 * ("winds up") and makes the elevator overshoot. Only let it add up when you're close to the goal.
 */
public class Challenge07MysteryPayload implements Controller {
  private final TunableNumber kP = new TunableNumber("kP", 0.0);
  private final TunableNumber kI = new TunableNumber("kI", 0.0);
  private final TunableNumber kD = new TunableNumber("kD", 0.0);
  private final TunableNumber kG = new TunableNumber("kG", 0.0);

  private final PIDController pid = new PIDController(0.0, 0.0, 0.0);

  @Override
  public void reset(Mechanism elevator) {
    // TODO: Stop the integral from winding up during big moves.
  }

  @Override
  public double calculate(Mechanism elevator) {
    pid.setPID(kP.get(), kI.get(), kD.get());
    // TODO: PID output plus kG, like the last challenge.
    return 0.0;
  }
}
