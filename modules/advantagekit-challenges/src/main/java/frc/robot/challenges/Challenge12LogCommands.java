package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.feeder.Feeder;
import frc.robot.subsystems.flywheel.Flywheel;

/**
 * Challenge 12: Log the Scheduler.
 *
 * <p>In a command-based robot, a lot of "why did it do that?" comes down to which command was
 * running. The {@code CommandScheduler} can tell you each time a command starts, finishes or gets
 * interrupted, so you can keep that in the log with everything else.
 *
 * <p>For each command, keep the key {@code "CommandsAll/<command name>"} true while it's running
 * and false when it isn't. The dashboard draws your keys as a timeline.
 */
public class Challenge12LogCommands extends LabChallenge {
  private final Flywheel flywheel = Flywheel.create();
  private final Feeder feeder = Feeder.create();

  private void logCommands() {
    // TODO: Call CommandScheduler.getInstance().onCommandInitialize(command -> ...) and record
    // true at "CommandsAll/" + command.getName().

    // TODO: Do the same with onCommandFinish, recording false.

    // TODO: There's a third way for a command to stop. Handle that one too.
  }

  @Override
  public void configureBindings() {
    logCommands();

    Command spinUp =
        flywheel.startEnd(() -> flywheel.setTargetRpm(3000.0), flywheel::stop).withName("SpinUp");
    Command eject =
        flywheel.startEnd(() -> flywheel.setTargetRpm(1000.0), flywheel::stop).withName("Eject");
    Command feed = feeder.feed().withTimeout(0.5).withName("Feed");

    controller.a().whileTrue(spinUp);
    controller.b().whileTrue(eject);
    controller.x().onTrue(feed);
  }
}
