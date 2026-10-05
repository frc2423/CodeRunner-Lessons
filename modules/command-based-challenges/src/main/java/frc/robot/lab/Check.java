package frc.robot.lab;

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
    if (log.frames.isEmpty()) {
      return fail("the trial didn't run");
    }
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

  /**
   * {@code test} holds for the whole window [from, to). On failure, {@code why} describes the first
   * frame where it didn't.
   */
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
          Frame bad = log.firstFailure(from, to, test);
          return bad == null ? pass(ok) : fail(f("at %.2f s: ", bad.time()) + why.apply(bad));
        });
  }

  /** {@link #always(String, double, double, Predicate, Function, String)}, for use inside all(). */
  static Check always(
      double from, double to, Predicate<Frame> test, Function<Frame, String> why, String ok) {
    return always("", from, to, test, why, ok);
  }

  /** {@code test} becomes true at some point in [from, to). */
  static Check becomes(
      String label,
      double from,
      double to,
      Predicate<Frame> test,
      Function<Frame, String> why,
      String okPrefix) {
    return of(
        label,
        log -> {
          double when = log.first(from, to, test);
          if (Double.isNaN(when)) {
            Frame end = log.at(to - World.DT);
            return fail(f("by %.2f s: ", to) + why.apply(end));
          }
          return pass(f("%s at %.2f s", okPrefix, when));
        });
  }

  /** Every check in {@code checks} passes; the details are joined. */
  static Check all(String label, Check... checks) {
    return of(
        label,
        log -> {
          StringBuilder detail = new StringBuilder();
          for (Check check : checks) {
            Outcome o = check.evaluate(log);
            if (!o.pass()) {
              return fail(o.detail());
            }
            if (!o.detail().isEmpty()) {
              if (detail.length() > 0) {
                detail.append(" · ");
              }
              detail.append(o.detail());
            }
          }
          return pass(detail.toString());
        });
  }

  static String rpm(double value) {
    return f("%.0f RPM", value);
  }
}
