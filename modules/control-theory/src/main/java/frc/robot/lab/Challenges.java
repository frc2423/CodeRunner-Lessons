package frc.robot.lab;

import frc.robot.challenges.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Every challenge the dashboard can run: its controller class, the mechanism it drives, the trial's
 * script, and what the trial is graded on.
 *
 * <p>The ids must match the dashboard's list of challenges. You don't need to edit this file.
 */
public final class Challenges {
  record Spec(
      String id,
      Supplier<Controller> factory,
      Supplier<Plant> plant,
      Scenario scenario,
      List<Check> checks,
      boolean sysId) {}

  private static final Map<String, Spec> SPECS = new LinkedHashMap<>();

  private static final double DEG = Math.PI / 180.0;

  static {
    add(
        "open-loop",
        Challenge00OpenLoop::new,
        Plant::flywheel,
        new Scenario(3.0).goal(0, 3000),
        Check.steadyError("Spins within 500 RPM of 3000 RPM", 2.0, 3.0, 500));

    add(
        "bang-bang",
        Challenge01BangBang::new,
        Plant::flywheel,
        new Scenario(4.5).goal(0, 3000).goal(2.5, 4500),
        Check.settles("Reaches 3000 RPM (±350) in under 0.6 s and stays there", 0, 2.5, 350, 0.6),
        Check.settles("Reaches 4500 RPM (±350) in under 0.6 s and stays there", 2.5, 4.5, 350, 0.6),
        Check.minVolts("Never drives the flywheel backwards (no negative volts)", 0.0));

    add(
        "proportional",
        Challenge02Proportional::new,
        Plant::flywheel,
        new Scenario(3.0).goal(0, 3000),
        Check.steadyError("Average error under 300 RPM once spun up", 1.5, 3.0, 300),
        Check.voltageNoise("Voltage stays steady (no chattering)", 1.5, 3.0, 0.3),
        Check.steadyError("Average error under 150 RPM", 1.5, 3.0, 150).bonus());

    Scenario feedforward =
        new Scenario(5.5).goal(0, 3000).goal(3.8, 4500).shot(1.6).shot(2.3).shot(3.0);
    add(
        "feedforward",
        Challenge03Feedforward::new,
        Plant::flywheel,
        feedforward,
        Check.steadyError("Holds 3000 RPM within 30 RPM (average)", 1.0, 1.6, 30),
        each(
            "Recovers from every shot (back within 50 RPM) in under 0.4 s",
            new double[] {1.6, 2.3, 3.0},
            3.8,
            50,
            0.4),
        Check.steadyError("Holds 4500 RPM within 30 RPM (average)", 4.8, 5.5, 30),
        each("Recovers from every shot in under 0.2 s", new double[] {1.6, 2.3, 3.0}, 3.8, 50, 0.2)
            .bonus());

    double[] steeringSteps = {0, 1.5, 3.0};
    add(
        "pd-steering",
        Challenge04PDSteering::new,
        Plant::steering,
        new Scenario(4.5).goal(0, 90).goal(1.5, -45).goal(3.0, 30),
        each("Settles within 2° in under 0.5 s after every turn", steeringSteps, 4.5, 2, 0.5),
        overshootEach("Overshoots by less than 5°", steeringSteps, 4.5, 5),
        each("Settles in under 0.3 s", steeringSteps, 4.5, 2, 0.3).bonus());

    add(
        "shortest-path",
        Challenge05ShortestPath::new,
        Plant::steering,
        new Scenario(4.5).start(150).goal(0, -150).goal(1.5, 100).goal(3.0, -100),
        each("Settles within 2° in under 0.5 s after every turn", steeringSteps, 4.5, 2, 0.5),
        Check.travel("Takes the short way every time (turns less than 360° in total)", 360));

    double[] elevatorSteps = {0, 2, 4};
    add(
        "fight-gravity",
        Challenge06FightGravity::new,
        Plant::elevator,
        new Scenario(6).goal(0, 0.5).goal(2, 1.2).goal(4, 0.3),
        Check.all(
            "Holds every height within 1 cm",
            Check.steadyError("", 1.5, 2.0, 0.01),
            Check.steadyError("", 3.5, 4.0, 0.01),
            Check.steadyError("", 5.5, 6.0, 0.01)),
        overshootEach("Overshoots by less than 3 cm", elevatorSteps, 6, 0.03),
        each("Settles within 1 cm in under 0.7 s", elevatorSteps, 6, 0.01, 0.7).bonus());

    add(
        "mystery-payload",
        Challenge07MysteryPayload::new,
        Plant::elevator,
        new Scenario(7.5).goal(0, 1.0).goal(5.0, 0.4).payload(2.0, true),
        Check.overshoot("Overshoots the first move by less than 2 cm (no windup)", 0, 2.0, 0.02),
        Check.steadyError("Holds 1 m within 0.5 cm before the payload", 1.5, 2.0, 0.005),
        Check.settles(
            "Back within 0.5 cm less than 1.5 s after grabbing the payload", 2.0, 5.0, 0.005, 1.5),
        Check.steadyError("Holds 0.4 m within 0.5 cm with the payload", 7.0, 7.5, 0.005));

    double[] armSteps = {0, 2, 4};
    add(
        "arm-feedforward",
        Challenge08ArmFeedforward::new,
        Plant::arm,
        new Scenario(6).start(-45 * DEG).goal(0, 0).goal(2, 90 * DEG).goal(4, 30 * DEG),
        Check.all(
            "Holds every angle within 1°",
            Check.steadyError("", 1.5, 2.0, 1 * DEG),
            Check.steadyError("", 3.5, 4.0, 1 * DEG),
            Check.steadyError("", 5.5, 6.0, 1 * DEG)),
        overshootEach("Overshoots by less than 5°", armSteps, 6, 5 * DEG),
        each("Settles within 1° in under 0.6 s", armSteps, 6, 1 * DEG, 0.6).bonus());

    double[] profileSteps = {0, 2.5};
    add(
        "motion-profile",
        Challenge09MotionProfile::new,
        Plant::elevator,
        new Scenario(5).goal(0, 1.4).goal(2.5, 0.2),
        Check.peakSpeed("Top speed stays under 1.5 m/s", 1.5, "m/s"),
        Check.peakAcceleration("Acceleration stays under 6 m/s² (no jerky starts)", 6, "m/s²"),
        each("Arrives within 1 cm in under 2 s", profileSteps, 5, 0.01, 2.0),
        overshootEach("Overshoots by less than 2 cm", profileSteps, 5, 0.02),
        overshootEach("Overshoots by less than 0.5 cm", profileSteps, 5, 0.005).bonus());

    add(
        "sysid",
        Challenge10SysId::new,
        Plant::mysteryFlywheel,
        new Scenario(8).noise(10, 0),
        true,
        Check.fitWithin("Finds kV within 5% of the true value", "kV", 0.05),
        Check.fitWithin("Finds kA within 15% of the true value", "kA", 0.15),
        Check.fitWithin("Finds kS within 25% of the true value", "kS", 0.25));

    add(
        "lqr",
        Challenge11LQR::new,
        Plant::mysteryFlywheel,
        new Scenario(5).goal(0, 2000).goal(1.5, 4000).shot(3.2).shot(3.9).noise(5, 0),
        each("Reaches every speed (±50 RPM) in under 0.5 s", new double[] {0, 1.5}, 3.2, 50, 0.5),
        Check.all(
            "Holds every speed within 20 RPM (average)",
            Check.steadyError("", 1.0, 1.5, 20),
            Check.steadyError("", 2.7, 3.2, 20)),
        each(
            "Recovers from every shot (back within 100 RPM) in under 0.3 s",
            new double[] {3.2, 3.9},
            5,
            100,
            0.3),
        Check.neverSaturates("Never asks for more than 12 V").bonus());

    add(
        "noisy-sensor",
        Challenge12NoisySensor::new,
        Plant::flywheel,
        new Scenario(5).goal(0, 3000).shot(2.5).shot(3.5).noise(120, 0),
        Check.voltageNoise("Voltage noise under 0.5 V while holding speed", 1.0, 2.5, 0.5),
        Check.steadyError("Holds 3000 RPM within 40 RPM (true speed, average)", 1.0, 2.5, 40),
        Check.estimateError(
            "Your filtered speed is within 60 RPM of the truth while holding (RMS)", 1.0, 2.5, 60),
        each(
            "Recovers from every shot (back within 150 RPM) in under 0.45 s",
            new double[] {2.5, 3.5},
            5,
            150,
            0.45));

    double[] kalmanSteps = {0, 1.5, 3.0};
    add(
        "kalman-filter",
        Challenge13KalmanFilter::new,
        Plant::mysteryFlywheel,
        new Scenario(5).goal(0, 2000).goal(1.5, 4000).goal(3.0, 2500).noise(120, 0),
        Check.estimateError("Your estimate is within 40 RPM of the truth (RMS)", 0.1, 5, 40),
        Check.all(
            "Voltage noise under 0.3 V while holding speed",
            Check.voltageNoise("", 1.0, 1.5, 0.3),
            Check.voltageNoise("", 2.5, 3.0, 0.3),
            Check.voltageNoise("", 4.0, 5.0, 0.3)),
        Check.all(
            "Holds every speed within 25 RPM (true speed, average)",
            Check.steadyError("", 1.0, 1.5, 25),
            Check.steadyError("", 2.5, 3.0, 25),
            Check.steadyError("", 4.0, 5.0, 25)),
        each("Reaches every speed (±100 RPM) in under 0.5 s", kalmanSteps, 5, 100, 0.5));

    double[] capstoneSteps = {0, 2.5, 5.0};
    add(
        "state-space-elevator",
        Challenge14StateSpaceElevator::new,
        Plant::elevator,
        new Scenario(7.5).goal(0, 1.2).goal(2.5, 0.4).goal(5.0, 1.5).noise(0, 0.004),
        Check.peakSpeed("Top speed stays under 1.5 m/s", 1.5, "m/s"),
        each("Arrives within 1 cm in under 1.6 s", capstoneSteps, 7.5, 0.01, 1.6),
        overshootEach("Overshoots by less than 1 cm", capstoneSteps, 7.5, 0.01),
        Check.estimateError(
            "Your estimate is within 0.3 cm of the true height (RMS)", 0, 7.5, 0.003),
        Check.voltageNoise("Voltage noise under 0.45 V", 0, 7.5, 0.45).bonus());
  }

  private Challenges() {}

  private static void add(
      String id,
      Supplier<Controller> factory,
      Supplier<Plant> plant,
      Scenario scenario,
      Check... checks) {
    add(id, factory, plant, scenario, false, checks);
  }

  private static void add(
      String id,
      Supplier<Controller> factory,
      Supplier<Plant> plant,
      Scenario scenario,
      boolean sysId,
      Check... checks) {
    SPECS.put(id, new Spec(id, factory, plant, scenario, List.of(checks), sysId));
  }

  /**
   * One settle check per goal change (or event) at {@code times}, each window ending at the next.
   */
  private static Check each(
      String label, double[] times, double end, double tolerance, double within) {
    List<Check> checks = new ArrayList<>();
    for (int i = 0; i < times.length; i++) {
      double to = i + 1 < times.length ? times[i + 1] : end;
      checks.add(Check.settles("", times[i], to, tolerance, within));
    }
    return Check.all(label, checks.toArray(new Check[0]));
  }

  private static Check overshootEach(String label, double[] times, double end, double limit) {
    List<Check> checks = new ArrayList<>();
    for (int i = 0; i < times.length; i++) {
      double to = i + 1 < times.length ? times[i + 1] : end;
      checks.add(Check.overshoot("", times[i], to, limit));
    }
    return Check.all(label, checks.toArray(new Check[0]));
  }

  /** Every challenge id, in order. */
  static List<String> ids() {
    return new ArrayList<>(SPECS.keySet());
  }

  /** The challenge with this id, or null. */
  static Spec get(String id) {
    return SPECS.get(id);
  }

  /** Makes a new controller for this challenge, so its tunable numbers belong to it. */
  static Controller create(Spec spec) {
    TunableNumber.setOwner(spec.id());
    try {
      return spec.factory().get();
    } finally {
      TunableNumber.setOwner("default");
    }
  }
}
