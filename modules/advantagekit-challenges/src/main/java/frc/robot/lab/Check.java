package frc.robot.lab;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * One thing a trial is graded on. Each check looks at the trial's log and says whether it passed,
 * with a short detail that helps the student see what went wrong.
 */
final class Check {
  record Outcome(boolean pass, String detail) {}

  final String label;
  final boolean bonus;
  private final Function<TrialLog, Outcome> test;

  private Check(String label, boolean bonus, Function<TrialLog, Outcome> test) {
    this.label = label;
    this.bonus = bonus;
    this.test = test;
  }

  static Check of(String label, Function<TrialLog, Outcome> test) {
    return new Check(label, false, test);
  }

  Check bonus() {
    return new Check(label, true, test);
  }

  Outcome evaluate(TrialLog log) {
    try {
      return test.apply(log);
    } catch (RuntimeException e) {
      return fail("couldn't check: " + e);
    }
  }

  static Outcome pass(String detail) {
    return new Outcome(true, detail);
  }

  static Outcome fail(String detail) {
    return new Outcome(false, detail);
  }

  static String f(String format, Object... args) {
    return String.format(Locale.ROOT, format, args);
  }

  // Common checks ---------------------------------------------------------------------------

  /** {@code test} holds for the whole window [from, to). */
  static Check always(
      String label,
      double from,
      double to,
      Predicate<Frame> test,
      Function<Frame, String> why,
      String ok) {
    return of(
        label,
        log -> {
          if (log.frames.isEmpty()) {
            return fail("the trial didn't run");
          }
          Frame bad = log.firstFailure(from, to, test);
          return bad == null ? pass(ok) : fail(f("at %.2f s: ", bad.time()) + why.apply(bad));
        });
  }

  /** {@code test} becomes true at some point in [from, to). */
  static Check becomes(
      String label,
      double from,
      double to,
      Predicate<Frame> test,
      Function<Frame, String> why,
      String ok) {
    return of(
        label,
        log -> {
          if (log.frames.isEmpty()) {
            return fail("the trial didn't run");
          }
          Frame found = log.first(from, to, test);
          return found != null
              ? pass(f("%s at %.2f s", ok, found.time()))
              : fail(f("by %.2f s: ", to) + why.apply(log.at(to)));
        });
  }

  /** Every check in {@code checks} passes; the details are joined. */
  static Check all(String label, Check... checks) {
    return of(
        label,
        log -> {
          StringBuilder detail = new StringBuilder();
          for (Check check : checks) {
            Outcome outcome = check.evaluate(log);
            if (!outcome.pass()) {
              return fail(outcome.detail());
            }
            if (!outcome.detail().isEmpty()) {
              if (detail.length() > 0) {
                detail.append(" · ");
              }
              detail.append(outcome.detail());
            }
          }
          return pass(detail.toString());
        });
  }

  /** A named output has this value (within {@code tolerance}) for the whole window. */
  static Check outputIs(
      String label, String key, double from, double to, double target, double tolerance) {
    return always(
        label,
        from,
        to,
        frame -> {
          Double value = frame.number(key);
          return value != null && Math.abs(value - target) <= tolerance;
        },
        frame -> describeOutput(frame, key),
        f("%s stayed at %.4g", key, target));
  }

  /** What an output held at this frame, for a failure message. */
  static String describeOutput(Frame frame, String key) {
    if (frame.output(key) == null) {
      return f("nothing is logged at \"%s\"", key);
    }
    Double number = frame.number(key);
    if (number != null) {
      return f("\"%s\" was %.4g", key, number);
    }
    return f(
        "\"%s\" is logged as %s, which isn't a number", key, LogAccess.typeName(frame.output(key)));
  }

  /** What an input held at this frame, for a failure message. */
  static String describeInput(Frame frame, String key) {
    if (frame.input(key) == null) {
      return f("there's no input at \"%s\": is processInputs called every loop?", key);
    }
    return f("the input \"%s\" was %.4g", key, frame.inputNumber(key));
  }

  // Replay checks -----------------------------------------------------------------------------

  /** Every output logged on the real run came out the same on the replay. */
  static Check replayMatches(String label) {
    return of(
        label,
        log -> {
          if (log.replay == null) {
            return fail(log.replayError.isEmpty() ? "the replay didn't run" : log.replayError);
          }
          String crash = log.replay.labText("ReplayError");
          if (!crash.isEmpty()) {
            return fail("your code crashed in the replay: " + crash);
          }
          if (log.replay.realKeys().isEmpty()) {
            return fail("the trial's log has no outputs to compare");
          }
          List<ReplayLog.Difference> differences = log.replay.differences();
          if (!differences.isEmpty()) {
            ReplayLog.Difference first = differences.get(0);
            return fail(
                f(
                    "%d output%s differ. First: \"%s\" at %.2f s was %s on the real run but %s in"
                        + " the replay",
                    differences.size(),
                    differences.size() == 1 ? "" : "s",
                    first.key(),
                    first.time(),
                    first.real(),
                    first.replay()));
          }
          int outputs = log.replay.realKeys().size();
          return pass(
              f(
                  "%d output%s matched over %d loops",
                  outputs, outputs == 1 ? "" : "s", log.replay.cycleCount()));
        });
  }

  /** Nothing touched the {@link Hardware} class during the replay. */
  static Check noHardwareInReplay(String label) {
    return of(
        label,
        log -> {
          if (log.replay == null) {
            return fail(log.replayError.isEmpty() ? "the replay didn't run" : log.replayError);
          }
          int touches = (int) log.replay.labNumber("HardwareTouches");
          if (touches > 0) {
            return fail(
                f(
                    "hardware was used %d times in the replay, first from %s",
                    touches, log.replay.labText("FirstTouch")));
          }
          return pass("the replay never touched the hardware");
        });
  }
}
