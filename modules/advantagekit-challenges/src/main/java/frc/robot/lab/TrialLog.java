package frc.robot.lab;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** What a trial recorded, for grading: every loop, and how its replay went. */
final class TrialLog {
  final List<Frame> frames = new ArrayList<>();
  final List<World.Shot> shots = new ArrayList<>();

  /** The replay of this trial, or null if the challenge has none or it didn't run. */
  ReplayLog replay = null;

  /** What went wrong with the replay, or "". */
  String replayError = "";

  /** The last frame before {@code time}, or the first frame if there is none. */
  Frame at(double time) {
    Frame found = frames.isEmpty() ? null : frames.get(0);
    for (Frame frame : frames) {
      if (frame.time() > time) {
        break;
      }
      found = frame;
    }
    return found;
  }

  /** The first frame in [from, to) where {@code test} fails, or null if it never does. */
  Frame firstFailure(double from, double to, Predicate<Frame> test) {
    for (Frame frame : frames) {
      if (frame.time() >= from && frame.time() < to && !test.test(frame)) {
        return frame;
      }
    }
    return null;
  }

  /** The first frame in [from, to) where {@code test} holds, or null if it never does. */
  Frame first(double from, double to, Predicate<Frame> test) {
    for (Frame frame : frames) {
      if (frame.time() >= from && frame.time() < to && test.test(frame)) {
        return frame;
      }
    }
    return null;
  }
}
