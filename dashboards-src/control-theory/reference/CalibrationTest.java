package frc.robot.lab;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.controller.ElevatorFeedforward;
import edu.wpi.first.math.controller.LinearQuadraticRegulator;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.estimator.KalmanFilter;
import edu.wpi.first.math.filter.LinearFilter;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N2;
import edu.wpi.first.math.system.LinearSystem;
import edu.wpi.first.math.system.LinearSystemLoop;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class CalibrationTest {
  static boolean run(String id, String name, Supplier<Controller> controller) {
    Challenges.Spec spec = Challenges.get(id);
    Trial trial = new Trial(spec, controller.get(), false);
    trial.start();
    while (!trial.finished()) {
      trial.step();
    }
    boolean passed = true;
    StringBuilder out = new StringBuilder();
    for (Check check : spec.checks()) {
      Check.Outcome o = check.evaluate(trial);
      if (!check.bonus) passed &= o.pass();
      out.append(
          String.format(
              "    %s %s%s: %s%n",
              o.pass() ? "PASS" : "FAIL", check.bonus ? "(bonus) " : "", check.label, o.detail()));
    }
    SysId.Fit fit = trial.fit();
    if (fit != null) {
      out.append(
          String.format(
              "    fit ok=%s %s kS=%.4g kV=%.4g kA=%.4g n=%d%n",
              fit.ok(), fit.problem(), fit.kS(), fit.kV(), fit.kA(), fit.samples()));
    }
    System.out.printf("%s [%s] %s%n%s", passed ? "OK  " : "NOPE", id, name, out);
    return passed;
  }

  /** One line per run: each check as P/F (bonus in brackets) with details. */
  static void line(String id, String name, Supplier<Controller> controller) {
    Challenges.Spec spec = Challenges.get(id);
    Trial trial = new Trial(spec, controller.get(), false);
    trial.start();
    while (!trial.finished()) {
      trial.step();
    }
    StringBuilder out = new StringBuilder();
    for (Check check : spec.checks()) {
      Check.Outcome o = check.evaluate(trial);
      String mark = o.pass() ? "P" : "F";
      out.append(check.bonus ? "[" + mark + "]" : mark)
          .append(" ")
          .append(o.detail())
          .append(" | ");
    }
    System.out.printf("SWEEP [%s] %-28s %s%n", id, name, out);
  }

  static Controller fn(java.util.function.ToDoubleFunction<Mechanism> f) {
    return f::applyAsDouble;
  }

  // ---------------------------------------------------------------------------------------------

  static Controller pdManual(double kP, double kD) {
    return new Controller() {
      double last;

      public void reset(Mechanism m) {
        last = m.goal() - m.position();
      }

      public double calculate(Mechanism m) {
        double e = m.goal() - m.position();
        double d = (e - last) / m.dt();
        last = e;
        return kP * e + kD * d;
      }
    };
  }

  static Controller pid(double kP, double kI, double kD, boolean wrap, double iZone, double kG) {
    return new Controller() {
      PIDController pid = new PIDController(kP, kI, kD);

      public void reset(Mechanism m) {
        if (wrap) pid.enableContinuousInput(-180, 180);
        if (iZone > 0) pid.setIZone(iZone);
      }

      public double calculate(Mechanism m) {
        return pid.calculate(m.position(), m.goal()) + kG;
      }
    };
  }

  static Controller armPid(double kP, double kD, double kG, boolean cosine) {
    return new Controller() {
      PIDController pid = new PIDController(kP, 0, kD);

      public double calculate(Mechanism m) {
        return pid.calculate(m.position(), m.goal()) + (cosine ? kG * Math.cos(m.position()) : kG);
      }
    };
  }

  static Controller profiled(double maxV, double maxA, double kP, double kD) {
    return new Controller() {
      PIDController pid = new PIDController(kP, 0, kD);
      ElevatorFeedforward ff = new ElevatorFeedforward(0.15, 0.45, 6.0, 0.6);
      TrapezoidProfile profile;
      TrapezoidProfile.State setpoint;

      public void reset(Mechanism m) {
        profile = new TrapezoidProfile(new TrapezoidProfile.Constraints(maxV, maxA));
        setpoint = new TrapezoidProfile.State(m.position(), 0);
      }

      public double calculate(Mechanism m) {
        TrapezoidProfile.State next =
            profile.calculate(m.dt(), setpoint, new TrapezoidProfile.State(m.goal(), 0));
        double volts =
            ff.calculateWithVelocities(setpoint.velocity, next.velocity)
                + pid.calculate(m.position(), next.position);
        setpoint = next;
        m.plot("Setpoint", setpoint.position);
        return volts;
      }
    };
  }

  static Controller lqr(double kS, double kV, double kA, double qelms, double relms) {
    return new Controller() {
      LinearQuadraticRegulator<N1, N1, N1> lqr;

      public void reset(Mechanism m) {
        LinearSystem<N1, N1, N1> plant = LinearSystemId.identifyVelocitySystem(kV, kA);
        lqr =
            new LinearQuadraticRegulator<>(
                plant, VecBuilder.fill(qelms), VecBuilder.fill(relms), m.dt());
      }

      public double calculate(Mechanism m) {
        double r = m.goal();
        double ff = kS * Math.signum(r) + kV * r;
        double fb = lqr.calculate(VecBuilder.fill(m.velocity()), VecBuilder.fill(r)).get(0, 0);
        return MathUtil.clamp(ff + fb, -12, 12);
      }
    };
  }

  static Controller filtered(double kS, double kV, double kP, double tc) {
    return new Controller() {
      LinearFilter filter;

      public void reset(Mechanism m) {
        filter = LinearFilter.singlePoleIIR(tc, m.dt());
      }

      public double calculate(Mechanism m) {
        double v = filter.calculate(m.velocity());
        m.showEstimate(v);
        return kS * Math.signum(m.goal()) + kV * m.goal() + kP * (m.goal() - v);
      }
    };
  }

  static Controller kalman(double kS, double kV, double kA, double modelStd, double kP) {
    return new Controller() {
      KalmanFilter<N1, N1, N1> kf;

      public void reset(Mechanism m) {
        LinearSystem<N1, N1, N1> plant = LinearSystemId.identifyVelocitySystem(kV, kA);
        kf =
            new KalmanFilter<>(
                Nat.N1(), Nat.N1(), plant, VecBuilder.fill(modelStd), VecBuilder.fill(120), m.dt());
      }

      public double calculate(Mechanism m) {
        double u = m.appliedVoltage() - kS * Math.signum(kf.getXhat(0));
        kf.predict(VecBuilder.fill(u), m.dt());
        kf.correct(VecBuilder.fill(u), VecBuilder.fill(m.velocity()));
        double v = kf.getXhat(0);
        m.showEstimate(v);
        double volts = kS * Math.signum(m.goal()) + kV * m.goal() + kP * (m.goal() - v);
        return MathUtil.clamp(volts, -12, 12);
      }
    };
  }

  static Controller stateSpace(
      double posTol, double velTol, double maxV, double maxA, double modelPos, double modelVel) {
    return new Controller() {
      LinearSystemLoop<N2, N1, N1> loop;
      TrapezoidProfile profile;
      TrapezoidProfile.State setpoint;

      @SuppressWarnings("unchecked")
      public void reset(Mechanism m) {
        LinearSystem<N2, N1, N1> plant =
            (LinearSystem<N2, N1, N1>) LinearSystemId.identifyPositionSystem(6.0, 0.6).slice(0);
        var lqr =
            new LinearQuadraticRegulator<>(
                plant, VecBuilder.fill(posTol, velTol), VecBuilder.fill(12.0), m.dt());
        var kf =
            new KalmanFilter<>(
                Nat.N2(),
                Nat.N1(),
                plant,
                VecBuilder.fill(modelPos, modelVel),
                VecBuilder.fill(0.004),
                m.dt());
        loop = new LinearSystemLoop<>(plant, lqr, kf, 12.0, m.dt());
        loop.reset(VecBuilder.fill(m.position(), 0));
        profile = new TrapezoidProfile(new TrapezoidProfile.Constraints(maxV, maxA));
        setpoint = new TrapezoidProfile.State(m.position(), 0);
      }

      public double calculate(Mechanism m) {
        setpoint = profile.calculate(m.dt(), setpoint, new TrapezoidProfile.State(m.goal(), 0));
        loop.setNextR(setpoint.position, setpoint.velocity);
        loop.correct(VecBuilder.fill(m.position()));
        m.showEstimate(loop.getXHat(0));
        loop.predict(m.dt());
        m.plot("Setpoint", setpoint.position);
        return loop.getU(0) + 0.45 + 0.15 * Math.signum(setpoint.velocity);
      }
    };
  }

  static Controller sysidTest(java.util.function.DoubleUnaryOperator volts) {
    return m -> volts.applyAsDouble(m.time());
  }

  static Controller profiledNow(double maxV, double maxA, double kP, double kD) {
    return new Controller() {
      PIDController pid = new PIDController(kP, 0, kD);
      ElevatorFeedforward ff = new ElevatorFeedforward(0.15, 0.45, 6.0, 0.6);
      TrapezoidProfile profile;
      TrapezoidProfile.State setpoint;

      public void reset(Mechanism m) {
        profile = new TrapezoidProfile(new TrapezoidProfile.Constraints(maxV, maxA));
        setpoint = new TrapezoidProfile.State(m.position(), 0);
      }

      public double calculate(Mechanism m) {
        TrapezoidProfile.State next =
            profile.calculate(m.dt(), setpoint, new TrapezoidProfile.State(m.goal(), 0));
        double volts =
            ff.calculateWithVelocities(setpoint.velocity, next.velocity)
                + pid.calculate(m.position(), setpoint.position);
        setpoint = next;
        return volts;
      }
    };
  }

  static Controller ffp(double kS, double kV, double kP) {
    return fn(m -> kS + kV * m.goal() + kP * (m.goal() - m.velocity()));
  }

  @Test
  void sweep3() {
    if (!"3".equals(System.getenv("SWEEP"))) return;
    for (double tc : new double[] {0.08, 0.1, 0.12}) {
      for (double kP : new double[] {0.005, 0.01, 0.015}) {
        line("noisy-sensor", "tc " + tc + " kP " + kP, () -> filtered(0.25, 0.002, kP, tc));
      }
    }
    for (double q : new double[] {50, 100, 300, 1000}) {
      line("kalman-filter", "sysid consts q " + q, () -> kalman(0.30, 0.00176, 0.00040, q, 0.01));
      line("kalman-filter", "off consts q " + q, () -> kalman(0.25, 0.0019, 0.00036, q, 0.01));
    }
    for (double q : new double[] {2500, 3000, 3500}) {
      line("lqr", "lqr q " + q, () -> lqr(0.32, 0.00175, 0.00042, q, 12));
      line("lqr", "lqr sysid q " + q, () -> lqr(0.30, 0.00176, 0.00040, q, 12));
    }
    line("state-space-elevator", "loop .06/1.2", () -> stateSpace(0.06, 1.2, 1.2, 4.0, 0.01, 0.1));
    line(
        "state-space-elevator",
        "loop .04/.8 m.005/.05",
        () -> stateSpace(0.04, 0.8, 1.2, 4.0, 0.005, 0.05));
    line(
        "state-space-elevator",
        "loop .02/.4 m.003/.03",
        () -> stateSpace(0.02, 0.4, 1.2, 4.0, 0.003, 0.03));
  }

  @Test
  void sweep2() {
    if (!"2".equals(System.getenv("SWEEP"))) return;
    for (double kP : new double[] {0, 0.005, 0.01, 0.02, 0.03, 0.05}) {
      line("feedforward", "exact ff kP " + kP, () -> ffp(0.25, 0.002, kP));
    }
    line("feedforward", "kV .0021 kS 0 kP .01", () -> ffp(0, 0.0021, 0.01));
    line("feedforward", "kV .0021 kS 0 kP .03", () -> ffp(0, 0.0021, 0.03));
    line("fight-gravity", "100/6", () -> pid(100, 0, 6, false, 0, 0.45));
    line("motion-profile", "next 30/1", () -> profiled(1.2, 4, 30, 1));
    line("motion-profile", "now 30/1", () -> profiledNow(1.2, 4, 30, 1));
    line("motion-profile", "default 1/2 next 30/1", () -> profiled(1.0, 2, 30, 1));
    for (double tc : new double[] {0.05, 0.08, 0.12, 0.2}) {
      line("noisy-sensor", "tc " + tc + " kP .01", () -> filtered(0.25, 0.002, 0.01, tc));
    }
    for (double q : new double[] {100, 300, 1000, 3000, 10000}) {
      for (double kP : new double[] {0.005, 0.01, 0.02}) {
        line("kalman-filter", "q " + q + " kP " + kP, () -> kalman(0.32, 0.00175, 0.00042, q, kP));
      }
    }
    for (double tc : new double[] {0.03, 0.05, 0.08, 0.12}) {
      for (double kP : new double[] {0.005, 0.01}) {
        line("kalman-filter", "iir " + tc + " kP " + kP, () -> filtered(0.32, 0.00175, kP, tc));
      }
    }
    for (double q : new double[] {1000, 2000, 4000}) {
      line("lqr", "lqr q " + q, () -> lqr(0.32, 0.00175, 0.00042, q, 12));
    }
    line("state-space-elevator", "loop .02/.4", () -> stateSpace(0.02, 0.4, 1.2, 4.0, 0.01, 0.1));
    line("state-space-elevator", "loop .04/.8", () -> stateSpace(0.04, 0.8, 1.2, 4.0, 0.01, 0.1));
    line("state-space-elevator", "loop .03/.3", () -> stateSpace(0.03, 0.3, 1.2, 4.0, 0.01, 0.1));
    line("sysid", "12 V", () -> sysidTest(t -> 12));
  }

  @Test
  void sweep() {
    if (!"1".equals(System.getenv("SWEEP"))) return;
    System.out.println("\n================ sweeps ================");
    for (double kP : new double[] {0, 0.005, 0.01, 0.02, 0.03, 0.05}) {
      line("feedforward", "exact ff kP " + kP, () -> ffp(0.25, 0.002, kP));
    }
    line("feedforward", "kV .0021 kS 0 kP .01", () -> ffp(0, 0.0021, 0.01));
    line("feedforward", "kV .0021 kS 0 kP .03", () -> ffp(0, 0.0021, 0.03));
    line("fight-gravity", "60/3", () -> pid(60, 0, 3, false, 0, 0.45));
    line("fight-gravity", "80/5", () -> pid(80, 0, 5, false, 0, 0.45));
    line("fight-gravity", "20/1", () -> pid(20, 0, 1, false, 0, 0.45));
    for (double kI : new double[] {30, 60, 100, 150, 250}) {
      line("mystery-payload", "kI " + kI + " iz .03", () -> pid(40, kI, 2, false, 0.03, 0.45));
    }
    line("mystery-payload", "kI 100 iz .05 kP 60", () -> pid(60, 100, 3, false, 0.05, 0.45));
    line("mystery-payload", "kI 100 no iz", () -> pid(40, 100, 2, false, 0, 0.45));
    line("arm-feedforward", "25/1.5", () -> armPid(25, 1.5, 0.9, true));
    line("motion-profile", "next 30/1", () -> profiled(1.2, 4, 30, 1));
    line("motion-profile", "next 10/0", () -> profiled(1.2, 4, 10, 0));
    line("motion-profile", "now 30/1", () -> profiledNow(1.2, 4, 30, 1));
    line("motion-profile", "now 10/0", () -> profiledNow(1.2, 4, 10, 0));
    line("motion-profile", "next 30/1 1.4/5", () -> profiled(1.4, 5, 30, 1));
    line("motion-profile", "default 1/2 next 30/1", () -> profiled(1.0, 2, 30, 1));
    for (double tc : new double[] {0.02, 0.05, 0.08, 0.12, 0.2}) {
      for (double kP : new double[] {0.005, 0.01, 0.02}) {
        line("noisy-sensor", "tc " + tc + " kP " + kP, () -> filtered(0.25, 0.002, kP, tc));
      }
    }
    for (double q : new double[] {10, 20, 40, 100, 200}) {
      for (double kP : new double[] {0.01, 0.02, 0.04}) {
        line("kalman-filter", "q " + q + " kP " + kP, () -> kalman(0.32, 0.00175, 0.00042, q, kP));
      }
    }
    for (double tc : new double[] {0.05, 0.08, 0.12}) {
      line("kalman-filter", "iir " + tc, () -> filtered(0.32, 0.00175, 0.01, tc));
    }
    for (double q : new double[] {50, 100, 200, 400}) {
      line("lqr", "lqr q " + q, () -> lqr(0.32, 0.00175, 0.00042, q, 12));
    }
    line("lqr", "lqr q 400 r 6", () -> lqr(0.32, 0.00175, 0.00042, 400, 6));
    line("lqr", "lqr sysid-ish", () -> lqr(0.30, 0.00176, 0.00040, 100, 12));
    line("state-space-elevator", "loop", () -> stateSpace(0.02, 0.4, 1.2, 4.0, 0.01, 0.1));
    line(
        "state-space-elevator",
        "loop m .005/.05",
        () -> stateSpace(0.02, 0.4, 1.2, 4.0, 0.005, 0.05));
    line("state-space-elevator", "loop m .02/.2", () -> stateSpace(0.02, 0.4, 1.2, 4.0, 0.02, 0.2));
    line(
        "state-space-elevator",
        "loop lqr .01/.2",
        () -> stateSpace(0.01, 0.2, 1.2, 4.0, 0.01, 0.1));
  }

  @Test
  void calibrate() {
    if (System.getenv("SWEEP") != null) return;
    System.out.println("\n================ reference solutions ================");
    run("open-loop", "example", () -> fn(m -> 6.0));
    run("bang-bang", "bang-bang", () -> fn(m -> m.velocity() < m.goal() ? 12 : 0));
    run("proportional", "kP .045", () -> fn(m -> 0.045 * (m.goal() - m.velocity())));
    run(
        "feedforward",
        "kS .25 kV .002 kP .03",
        () -> fn(m -> 0.25 + 0.002 * m.goal() + 0.03 * (m.goal() - m.velocity())));
    run(
        "feedforward",
        "kS .25 kV .002 kP .05",
        () -> fn(m -> 0.25 + 0.002 * m.goal() + 0.05 * (m.goal() - m.velocity())));
    run("pd-steering", "PD .2/.012", () -> pdManual(0.2, 0.012));
    run("pd-steering", "PD .3/.02", () -> pdManual(0.3, 0.02));
    run("shortest-path", "PID wrap", () -> pid(0.2, 0, 0.012, true, 0, 0));
    run("fight-gravity", "PD+kG", () -> pid(40, 0, 2, false, 0, 0.45));
    run("mystery-payload", "PID izone", () -> pid(40, 60, 2, false, 0.03, 0.45));
    run("arm-feedforward", "PD+kGcos", () -> armPid(15, 1, 0.9, true));
    run("motion-profile", "profile 1.2/4", () -> profiled(1.2, 4, 30, 1));
    run("sysid", "ramp + steps", () -> sysidTest(t -> t < 4 ? 1.5 * t : t < 6 ? 10 : 2));
    run("lqr", "lqr truth 50/12", () -> lqr(0.32, 0.00175, 0.00042, 50, 12));
    run("noisy-sensor", "iir .1 kP .01", () -> filtered(0.25, 0.002, 0.01, 0.1));
    run("kalman-filter", "kf sysid 100 kP .01", () -> kalman(0.30, 0.00176, 0.00040, 100, 0.01));
    run("state-space-elevator", "loop", () -> stateSpace(0.02, 0.4, 1.2, 4.0, 0.01, 0.1));

    System.out.println("\n================ naive attempts (should fail) ================");
    run("proportional", "kP .01", () -> fn(m -> 0.01 * (m.goal() - m.velocity())));
    run("proportional", "kP .08", () -> fn(m -> 0.08 * (m.goal() - m.velocity())));
    run("feedforward", "P only .05", () -> fn(m -> 0.05 * (m.goal() - m.velocity())));
    run("feedforward", "FF only", () -> fn(m -> 0.25 + 0.002 * m.goal()));
    run(
        "feedforward",
        "FF kV .0021",
        () -> fn(m -> 0.0021 * m.goal() + 0.01 * (m.goal() - m.velocity())));
    run("pd-steering", "P .2", () -> pdManual(0.2, 0));
    run("pd-steering", "P .03", () -> pdManual(0.03, 0));
    run("shortest-path", "no wrap", () -> pid(0.2, 0, 0.012, false, 0, 0));
    run("fight-gravity", "PD no kG", () -> pid(40, 0, 2, false, 0, 0));
    run("mystery-payload", "PD+kG no I", () -> pid(40, 0, 2, false, 0, 0.45));
    run("mystery-payload", "PID no izone", () -> pid(40, 60, 2, false, 0, 0.45));
    run("arm-feedforward", "PD+const kG", () -> armPid(15, 1, 0.9, false));
    run("motion-profile", "plain PID", () -> pid(30, 0, 1, false, 0, 0.45));
    run("sysid", "12 V", () -> sysidTest(t -> 12));
    run("sysid", "slow ramp", () -> sysidTest(t -> 1.2 * t));
    run("sysid", "one step", () -> sysidTest(t -> t < 4 ? 8 : 0));
    run(
        "lqr",
        "P .03 + ff truth",
        () -> fn(m -> 0.32 + 0.00175 * m.goal() + 0.03 * (m.goal() - m.velocity())));
    run("noisy-sensor", "no filter", () -> filtered(0.25, 0.002, 0.01, 0.0001));
    run("noisy-sensor", "iir .4", () -> filtered(0.25, 0.002, 0.01, 0.4));
    run("kalman-filter", "iir .08", () -> filtered(0.32, 0.00175, 0.01, 0.08));
    run("state-space-elevator", "profiled PID, raw", () -> profiled(1.2, 4, 30, 1));
  }
}
