package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.lab.CommandChallenge;

/**
 * Example: Hello, Commands. This one is already done: read it, then run it from the Command Lab.
 *
 * <p>A <b>command</b> is one job for the robot, like "spin the intake rollers". A <b>trigger</b> is
 * something that can be true or false, like "the X button is pressed". Binding a command to a
 * trigger tells the robot when to do the job.
 *
 * <p>Every challenge extends {@link CommandChallenge}, which gives it the robot's subsystems
 * ({@code drive}, {@code intake}, {@code indexer}, {@code shooter} and {@code leds}) and the
 * driver's {@code controller}.
 */
public class Challenge00Example extends CommandChallenge {
  @Override
  public void configureBindings() {
    // A command that spins the intake rollers while it runs, and stops them when it ends.
    // intake.runEnd(...) makes a command that *requires* the intake: while it runs, no other
    // command can use the intake.
    Command spinRollers =
        intake
            .runEnd(() -> intake.setRollers(1.0), () -> intake.stopRollers())
            .withName("Spin rollers");

    // While X is held, run that command. When X is let go, the command ends.
    controller.x().whileTrue(spinRollers);

    // When Y is pressed, print a message (look for it in the robot program's console).
    controller.y().onTrue(Commands.print("Hello from a command!"));
  }
}
