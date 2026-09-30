package frc.robot.lab;

import java.util.function.Function;

/**
 * One thing a trial is graded on, like "settles in under 0.5 s". Times are seconds since the trial
 * started, and amounts are in the mechanism's units (see {@link Mechanism}).
 */
final class Check {
  record Outcome(boolean pass, String detail) {}

  final String label;
  private final Function<Trial, Outcome> rule;
  boolean bonus = false;

  private Check(String label, Function<Trial, Outcome> rule) {
    this.label = label;
    this.rule = rule;
  }

  /** Marks this check as a bonus: it earns a star, but isn't needed to complete the challenge. */
  Check bonus() {
    bonus = true;
    return this;
  }

  Outcome evaluate(Trial trial) {
    if (trial.trace.size() == 0) {
      return new Outcome(false, "the trial did not run");
    }
    return rule.apply(trial);
  }

  // Rules
  // ------------------------------------------------------------------------------------------

  /** Passes when every one of {@code checks} passes, like one check per goal change. */
  static Check all(String label, Check... checks) {
    return new Check(
        label,
        trial -> {
          boolean pass = true;
          StringBuilder details = new StringBuilder();
          for (Check check : checks) {
            Outcome outcome = check.evaluate(trial);
            pass &= outcome.pass();
            if (details.length() > 0) {
              details.append(" · ");
            }
            details.append(outcome.detail());
          }
          return new Outcome(pass, details.toString());
        });
  }

  /**
   * After {@code from}, the output gets within {@code tolerance} of the goal and stays there until
   * {@code to}, in at most {@code within} seconds.
   */
  static Check settles(String label, double from, double to, double tolerance, double within) {
    return new Check(
        label,
        trial -> {
          Trace trace = trial.trace;
          int start = trace.indexAt(from);
          int end = trace.indexAt(to);
          int settled = end;
          for (int i = end - 1; i >= start; i--) {
            if (Math.abs(trace.error.get(i)) > tolerance) {
              break;
            }
            settled = i;
          }
          if (settled >= end) {
            double off = Math.abs(trace.error.get(Math.max(end - 1, 0)));
            return new Outcome(false, "never settled: " + trial.format(off) + " off at the end");
          }
          double seconds = trace.time.get(settled) - from;
          return new Outcome(seconds <= within + 1e-9, String.format("settled in %.2f s", seconds));
        });
  }

  /** Between {@code from} and {@code to}, the output never goes past the goal by more than this. */
  static Check overshoot(String label, double from, double to, double limit) {
    return new Check(
        label,
        trial -> {
          Trace trace = trial.trace;
          int start = trace.indexAt(from);
          int end = trace.indexAt(to);
          if (start >= end) {
            return new Outcome(false, "the trial did not get this far");
          }
          // Which way the output had to move to reach the goal.
          double direction = Math.signum(trace.error.get(start));
          double worst = 0.0;
          for (int i = start; i < end; i++) {
            worst = Math.max(worst, -trace.error.get(i) * direction);
          }
          return new Outcome(
              worst <= limit + 1e-9,
              worst < 1e-6 ? "no overshoot" : "overshot by " + trial.format(worst));
        });
  }

  /** The average distance from the goal between {@code from} and {@code to} is under the limit. */
  static Check steadyError(String label, double from, double to, double limit) {
    return new Check(
        label,
        trial -> {
          double average = average(trial.trace, trial.trace.error, from, to, Math::abs);
          return new Outcome(average <= limit, "average error " + trial.format(average));
        });
  }

  /** The mechanism's true speed never goes over the limit. */
  static Check peakSpeed(String label, double limit, String units) {
    return new Check(
        label,
        trial -> {
          double peak = 0.0;
          for (int i = 0; i < trial.trace.size(); i++) {
            peak = Math.max(peak, Math.abs(trial.trace.velocity.get(i)));
          }
          return new Outcome(peak <= limit, String.format("top speed %.2f %s", peak, units));
        });
  }

  /** The mechanism's true acceleration never goes over the limit. */
  static Check peakAcceleration(String label, double limit, String units) {
    return new Check(
        label,
        trial -> {
          Trace trace = trial.trace;
          double peak = 0.0;
          for (int i = 1; i < trace.size(); i++) {
            double acceleration =
                (trace.velocity.get(i) - trace.velocity.get(i - 1)) / Mechanism.DT;
            peak = Math.max(peak, Math.abs(acceleration));
          }
          return new Outcome(
              peak <= limit, String.format("peak acceleration %.1f %s", peak, units));
        });
  }

  /** The controller never asks for less than {@code min} volts. */
  static Check minVolts(String label, double min) {
    return new Check(
        label,
        trial -> {
          double lowest = Double.POSITIVE_INFINITY;
          for (int i = 0; i < trial.trace.size(); i++) {
            lowest = Math.min(lowest, trial.trace.volts.get(i));
          }
          return new Outcome(lowest >= min - 1e-9, String.format("lowest %.1f V", lowest));
        });
  }

  /** The controller never hits the battery's 12 volt limit. */
  static Check neverSaturates(String label) {
    return new Check(
        label,
        trial -> {
          double peak = 0.0;
          for (int i = 0; i < trial.trace.size(); i++) {
            peak = Math.max(peak, Math.abs(trial.trace.volts.get(i)));
          }
          return new Outcome(peak < 11.99, String.format("peak %.1f V", peak));
        });
  }

  /**
   * How much the voltage jumps around from one loop to the next between {@code from} and {@code
   * to}: the RMS of the loop-to-loop change divided by √2, which equals the standard deviation for
   * random noise.
   */
  static Check voltageNoise(String label, double from, double to, double limit) {
    return new Check(
        label,
        trial -> {
          Trace trace = trial.trace;
          int start = Math.max(trace.indexAt(from), 1);
          int end = trace.indexAt(to);
          double sum = 0.0;
          int count = 0;
          for (int i = start; i < end; i++) {
            double change = trace.volts.get(i) - trace.volts.get(i - 1);
            sum += change * change;
            count++;
          }
          double noise = count == 0 ? 0.0 : Math.sqrt(sum / count / 2.0);
          return new Outcome(noise <= limit, String.format("voltage noise %.2f V", noise));
        });
  }

  /** The mechanism turns less than {@code limit} in total (steering). */
  static Check travel(String label, double limit) {
    return new Check(
        label,
        trial -> {
          Trace trace = trial.trace;
          double total = 0.0;
          for (int i = 1; i < trace.size(); i++) {
            total += Math.abs(trace.position.get(i) - trace.position.get(i - 1));
          }
          return new Outcome(total <= limit, "turned " + trial.format(total) + " in total");
        });
  }

  /**
   * The controller's {@link Mechanism#showEstimate} stays close to the true output: RMS error
   * between {@code from} and {@code to} under the limit.
   */
  static Check estimateError(String label, double from, double to, double limit) {
    return new Check(
        label,
        trial -> {
          Trace trace = trial.trace;
          int start = trace.indexAt(from);
          int end = trace.indexAt(to);
          double sum = 0.0;
          int count = 0;
          for (int i = start; i < end; i++) {
            double estimate = trace.estimate.get(i);
            if (Double.isNaN(estimate)) {
              return new Outcome(false, "no estimate: call mechanism.showEstimate(…) every loop");
            }
            double error = estimate - trace.output.get(i);
            sum += error * error;
            count++;
          }
          double rms = count == 0 ? 0.0 : Math.sqrt(sum / count);
          return new Outcome(rms <= limit, "estimate off by " + trial.format(rms) + " (RMS)");
        });
  }

  /** The SysId fit found this constant within {@code fraction} of the true value. */
  static Check fitWithin(String label, String constant, double fraction) {
    return new Check(
        label,
        trial -> {
          SysId.Fit fit = trial.fit();
          if (fit == null || !fit.ok()) {
            return new Outcome(false, fit == null ? "no fit" : fit.problem());
          }
          double found =
              switch (constant) {
                case "kS" -> fit.kS();
                case "kV" -> fit.kV();
                default -> fit.kA();
              };
          double truth =
              switch (constant) {
                case "kS" -> trial.plant.kS;
                case "kV" -> trial.plant.kV;
                default -> trial.plant.kA;
              };
          double off = Math.abs(found - truth) / truth;
          return new Outcome(
              off <= fraction, String.format("found %.4g, %.0f%% off", found, off * 100));
        });
  }

  private static double average(
      Trace trace, Trace.Series series, double from, double to, Function<Double, Double> f) {
    int start = trace.indexAt(from);
    int end = trace.indexAt(to);
    double sum = 0.0;
    for (int i = start; i < end; i++) {
      sum += f.apply(series.get(i));
    }
    return end > start ? sum / (end - start) : Double.POSITIVE_INFINITY;
  }
}
