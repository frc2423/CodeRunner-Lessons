package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.feeder.Feeder;
import frc.robot.subsystems.flywheel.Flywheel;
import org.littletonrobotics.junction.Logger;

public class Challenge12LogCommands extends LabChallenge {
  private final Flywheel flywheel = Flywheel.create();
  private final Feeder feeder = Feeder.create();

  private void logCommands() {
    CommandScheduler scheduler = CommandScheduler.getInstance();
    scheduler.onCommandInitialize(
        command -> Logger.recordOutput("CommandsAll/" + command.getName(), true));
    scheduler.onCommandFinish(
        command -> Logger.recordOutput("CommandsAll/" + command.getName(), false));
    scheduler.onCommandInterrupt(
        command -> Logger.recordOutput("CommandsAll/" + command.getName(), false));
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
