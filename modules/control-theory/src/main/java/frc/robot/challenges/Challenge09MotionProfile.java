package frc.robot.challenges;

import edu.wpi.first.math.controller.ElevatorFeedforward;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import frc.robot.lab.Controller;
import frc.robot.lab.Mechanism;
import frc.robot.lab.TunableNumber;

/**
 * Challenge 9: Motion Profile.
 *
 * <p>Jumping the goal straight from the bottom to the top slams the elevator at full power: too
 * fast, and too jerky for the robot (and anyone near it). A <b>motion profile</b> plans a smooth
 * path instead: speed up gently, cruise, slow down gently. Each loop, you ask the profile where the
 * carriage should be <i>right now</i>, and follow that with a feedforward and PID. WPILib's {@link
 * TrapezoidProfile} does the planning.
 *
 * <p>The elevator's constants, measured with SysId: kS = 0.15 V, kG = 0.45 V, kV = 6.0 V/(m/s), kA
 * = 0.6 V/(m/s²).
 */
public class Challenge09MotionProfile implements Controller {
  private final TunableNumber kP = new TunableNumber("kP", 0.0);
  private final TunableNumber kD = new TunableNumber("kD", 0.0);
  private final TunableNumber maxVelocity = new TunableNumber("maxVelocity", 1.0);
  private final TunableNumber maxAcceleration = new TunableNumber("maxAcceleration", 2.0);

  private final PIDController pid = new PIDController(0.0, 0.0, 0.0);
  private final ElevatorFeedforward feedforward = new ElevatorFeedforward(0.15, 0.45, 6.0, 0.6);

  // TODO: Add fields for the profile, and for the setpoint it gave you last loop.

  @Override
  public void reset(Mechanism elevator) {
    // TODO: Make the TrapezoidProfile from the max velocity and acceleration, and start the
    // setpoint where the elevator is now.
  }

  @Override
  public double calculate(Mechanism elevator) {
    pid.setPID(kP.get(), 0.0, kD.get());
    // TODO: Step the profile toward elevator.goal(), then feedforward + PID toward the new
    // setpoint.
    return 0.0;
  }
}
