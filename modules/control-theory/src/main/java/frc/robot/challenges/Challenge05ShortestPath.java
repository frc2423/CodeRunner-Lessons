package frc.robot.challenges;

import edu.wpi.first.math.controller.PIDController;
import frc.robot.lab.Controller;
import frc.robot.lab.Mechanism;
import frc.robot.lab.TunableNumber;

/**
 * Challenge 5: Shortest Path.
 *
 * <p>The wheel's angle goes from -180 to 180 degrees, and those two are the same direction. To get
 * from 150° to -150°, the short way is 60° through 180, not 300° the long way round.
 *
 * <p>This time, use WPILib's {@link PIDController} instead of writing the math yourself. It can
 * handle the wrap-around for you.
 */
public class Challenge05ShortestPath implements Controller {
  private final TunableNumber kP = new TunableNumber("kP", 0.0);
  private final TunableNumber kD = new TunableNumber("kD", 0.0);

  private final PIDController pid = new PIDController(0.0, 0.0, 0.0);

  @Override
  public void reset(Mechanism wheel) {
    // TODO: Tell the PIDController that -180 and 180 degrees are the same angle.
  }

  @Override
  public double calculate(Mechanism wheel) {
    pid.setPID(kP.get(), 0.0, kD.get());
    // TODO: Return the PIDController's output for this position and goal.
    return 0.0;
  }
}
