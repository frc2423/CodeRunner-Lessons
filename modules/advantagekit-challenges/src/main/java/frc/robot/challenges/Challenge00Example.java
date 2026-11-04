package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.lab.LabChallenge;
import org.littletonrobotics.junction.Logger;

/**
 * Example: Hello, Logger. This one is finished, so you can see how a challenge works.
 *
 * <p>Pressing A adds one to a counter and writes the counter to the log. Run it and watch {@code
 * Hello/Presses} appear in the <b>log view</b> on the dashboard. Then open AdvantageScope: the same
 * value is under {@code AdvantageKit/RealOutputs/Hello/Presses}.
 */
public class Challenge00Example extends LabChallenge {
  private int presses = 0;

  @Override
  public void configureBindings() {
    controller
        .a()
        .onTrue(
            Commands.runOnce(
                () -> {
                  presses++;
                  Logger.recordOutput("Hello/Presses", presses);
                }));
  }
}
