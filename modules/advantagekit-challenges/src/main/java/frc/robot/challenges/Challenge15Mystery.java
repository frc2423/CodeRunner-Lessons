package frc.robot.challenges;

import frc.robot.Constants;
import frc.robot.challenges.mystery.Shooter;
import frc.robot.challenges.mystery.ShooterIO;
import frc.robot.challenges.mystery.ShooterIOSim;
import frc.robot.lab.LabChallenge;

/**
 * Challenge 15: The Qual 17 Mystery.
 *
 * <p>In qualification match 17, your robot took three shots, and one of them missed badly. The
 * drive team swears they did nothing different. The robot's been taken apart since then, so all you
 * have is the code in {@code challenges/mystery/} and the log in {@code
 * src/main/deploy/match-logs/qual-17.wpilog}.
 *
 * <p>Find out why it missed. Fix the bug, and prove your fix would have worked by replaying the
 * match. Nothing's marked TODO: this one's all yours.
 */
public class Challenge15Mystery extends LabChallenge {
  private final Shooter shooter =
      new Shooter(
          Constants.currentMode == Constants.Mode.REPLAY ? new ShooterIO() {} : new ShooterIOSim());

  @Override
  public void configureBindings() {
    controller.x().onTrue(shooter.runOnce(() -> shooter.setTargetRpm(Shooter.NEAR_RPM)));
    controller.y().onTrue(shooter.runOnce(() -> shooter.setTargetRpm(Shooter.FAR_RPM)));
    controller.a().onTrue(shooter.fire());
  }
}
