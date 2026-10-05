package frc.robot.lab;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Subsystem;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

/**
 * Watches the command scheduler: which commands start, finish or get interrupted, and what they
 * require. The dashboard draws this as the command timeline.
 */
final class CommandTracker {
  enum Type {
    START("start"),
    FINISH("finish"),
    INTERRUPT("interrupt");

    final String id;

    Type(String id) {
      this.id = id;
    }
  }

  /** One thing that happened to a command. {@code by} names the interrupting command, if any. */
  record Event(
      int seq,
      int run,
      double time,
      Type type,
      String name,
      List<String> requirements,
      String by) {}

  private static final int KEEP = 80;

  private final DoubleSupplier clock;
  private final IntSupplier run;
  private final Set<Command> active = new LinkedHashSet<>();
  private final List<Event> recent = new ArrayList<>();
  private List<Event> recording = null;
  private int seq = 0;

  CommandTracker(DoubleSupplier clock, IntSupplier run) {
    this.clock = clock;
    this.run = run;
    CommandScheduler scheduler = CommandScheduler.getInstance();
    scheduler.onCommandInitialize(
        command -> {
          active.add(command);
          add(Type.START, command, null);
        });
    scheduler.onCommandFinish(
        command -> {
          active.remove(command);
          add(Type.FINISH, command, null);
        });
    scheduler.onCommandInterrupt(
        (Command command, Optional<Command> interruptor) -> {
          active.remove(command);
          add(Type.INTERRUPT, command, interruptor.map(CommandTracker::name).orElse(""));
        });
  }

  private void add(Type type, Command command, String by) {
    Event event =
        new Event(
            ++seq,
            run.getAsInt(),
            clock.getAsDouble(),
            type,
            name(command),
            requirements(command),
            by);
    recent.add(event);
    while (recent.size() > KEEP) {
      recent.remove(0);
    }
    if (recording != null) {
      recording.add(event);
    }
  }

  /** Starts keeping every event for a trial's grading. */
  void record(List<Event> into) {
    recording = into;
  }

  void stopRecording() {
    recording = null;
  }

  /** Forgets what's running; used after the scheduler is reset. */
  void clearActive() {
    active.clear();
  }

  List<Event> recent() {
    return recent;
  }

  /** The commands that are scheduled right now. */
  List<Command> active() {
    return new ArrayList<>(active);
  }

  static String name(Command command) {
    String name = command.getName();
    return name == null || name.isBlank() ? "(unnamed command)" : name;
  }

  static List<String> requirements(Command command) {
    List<String> names = new ArrayList<>();
    for (Subsystem subsystem : command.getRequirements()) {
      names.add(subsystemName(subsystem));
    }
    names.sort(null);
    return names;
  }

  static String subsystemName(Subsystem subsystem) {
    String name = subsystem.getName();
    return name == null || name.isBlank() ? subsystem.getClass().getSimpleName() : name;
  }

  /** WPILib's default names for commands made by factories, which say nothing about the job. */
  static boolean isDefaultName(String name) {
    return switch (name) {
      case "InstantCommand",
          "RunCommand",
          "StartEndCommand",
          "FunctionalCommand",
          "SequentialCommandGroup",
          "ParallelCommandGroup",
          "ParallelRaceGroup",
          "ParallelDeadlineGroup",
          "WaitCommand",
          "WaitUntilCommand",
          "ConditionalCommand",
          "RepeatCommand",
          "WrapperCommand",
          "(unnamed command)" -> true;
      default -> false;
    };
  }
}
