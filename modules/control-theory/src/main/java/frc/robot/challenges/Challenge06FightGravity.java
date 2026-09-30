package frc.robot.challenges;

import edu.wpi.first.math.controller.PIDController;
import frc.robot.lab.Controller;
import frc.robot.lab.Mechanism;
import frc.robot.lab.TunableNumber;

/**
 * Challenge 6: Fight Gravity.
 *
 * <p>An elevator, with its height in meters. Gravity pulls the carriage down all the time, so a PD
 * controller alone sags below its goal. Add a <b>gravity feedforward</b>: the voltage that holds
 * the carriage still, {@code kG}, added on top of the PID output.
 */
public class Challenge06FightGravity implements Controller {
  private final TunableNumber kP = new TunableNumber("kP", 0.0);
  private final TunableNumber kD = new TunableNumber("kD", 0.0);
  private final TunableNumber kG = new TunableNumber("kG", 0.0);

  private final PIDController pid = new PIDController(0.0, 0.0, 0.0);

  @Override
  public double calculate(Mechanism elevator) {
    pid.setPID(kP.get(), 0.0, kD.get());
    // TODO: PID output plus kG.
    return 0.0;
  }
}
