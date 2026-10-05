package frc.robot.lab;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** What happened during a trial: a frame every loop, and every command event. */
final class TrialLog {
  final List<Frame> frames = new ArrayList<>();
  final List<CommandTracker.Event> events = new ArrayList<>();
  final List<World.Shot> shots = new ArrayList<>();

  /** The frame at (or just after) {@code time}. */
  Frame at(double time) {
    for (Frame frame : frames) {
      if (frame.time() >= time - 1e-6) {
        return frame;
      }
    }
    return last();
  }

  Frame last() {
    return frames.isEmpty() ? null : frames.get(frames.size() - 1);
  }

  /** Every frame from {@code from} up to (not including) {@code to}. */
  List<Frame> between(double from, double to) {
    List<Frame> out = new ArrayList<>();
    for (Frame frame : frames) {
      if (frame.time() >= from - 1e-6 && frame.time() < to - 1e-6) {
        out.add(frame);
      }
    }
    return out;
  }

  /** The first time in [from, to) that {@code test} holds, or NaN. */
  double first(double from, double to, Predicate<Frame> test) {
    for (Frame frame : between(from, to)) {
      if (test.test(frame)) {
        return frame.time();
      }
    }
    return Double.NaN;
  }

  /** The first frame in [from, to) where {@code test} fails, or null if it always holds. */
  Frame firstFailure(double from, double to, Predicate<Frame> test) {
    for (Frame frame : between(from, to)) {
      if (!test.test(frame)) {
        return frame;
      }
    }
    return null;
  }

  /** Command events of this type in [from, to). */
  List<CommandTracker.Event> events(CommandTracker.Type type, double from, double to) {
    List<CommandTracker.Event> out = new ArrayList<>();
    for (CommandTracker.Event event : events) {
      if (event.type() == type && event.time() >= from - 1e-6 && event.time() < to - 1e-6) {
        out.add(event);
      }
    }
    return out;
  }

  /** Shots launched in [from, to). */
  List<World.Shot> shots(double from, double to) {
    List<World.Shot> out = new ArrayList<>();
    for (World.Shot shot : shots) {
      if (shot.launched() >= from - 1e-6 && shot.launched() < to - 1e-6) {
        out.add(shot);
      }
    }
    return out;
  }
}
