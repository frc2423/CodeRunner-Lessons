package frc.robot.lab;

import edu.wpi.first.math.MathUtil;
import java.util.List;
import java.util.Random;

/**
 * One run of a controller on a simulated mechanism. A graded trial follows its challenge's {@link
 * Scenario} and records a {@link Trace}; a sandbox runs until stopped, with the goal and events
 * coming from the dashboard.
 *
 * <p>Time in a trial counts robot loops, not the wall clock, so a trial gives the same result
 * however busy the computer is.
 */
final class Trial {
  static final double MAX_VOLTS = 12.0;

  final Challenges.Spec spec;
  final Plant plant;
  final Controller controller;
  final boolean sandbox;
  final Trace trace = new Trace();
  final Mechanism mechanism = new Mechanism();

  private final Random random = new Random(1678);
  private int loop = 0;
  private int nextEvent = 0;
  private double appliedVolts = 0.0;
  private double requestedVolts = 0.0;
  private double goal;
  private int shots = 0;
  private SysId.Fit fit;
  private boolean fitDone = false;

  Trial(Challenges.Spec spec, Controller controller, boolean sandbox) {
    this.spec = spec;
    this.plant = spec.plant().get();
    this.controller = controller;
    this.sandbox = sandbox;
    Scenario scenario = spec.scenario();
    plant.reset(scenario.startPosition, scenario.startVelocity);
    goal = scenario.goalAt(0.0);
    measure();
  }

  double time() {
    return loop * Mechanism.DT;
  }

  double goal() {
    return goal;
  }

  /** Sets the goal (sandbox only). */
  void setGoal(double goal) {
    this.goal = goal;
  }

  double appliedVolts() {
    return appliedVolts;
  }

  double requestedVolts() {
    return requestedVolts;
  }

  int shots() {
    return shots;
  }

  boolean finished() {
    return !sandbox && time() >= spec.scenario().duration - 1e-9;
  }

  /** Calls the controller's reset. Throws whatever the controller throws. */
  void start() {
    controller.reset(mechanism);
  }

  /**
   * Runs one robot loop: applies due events, asks the controller for a voltage, records the sample,
   * and moves the mechanism forward. Throws whatever the controller throws.
   */
  void step() {
    Scenario scenario = spec.scenario();
    if (!sandbox) {
      goal = scenario.goalAt(time());
      List<Scenario.Event> events = scenario.events;
      while (nextEvent < events.size() && events.get(nextEvent).time() <= time() + 1e-9) {
        apply(events.get(nextEvent++));
      }
    }
    measure();

    double volts = controller.calculate(mechanism);
    if (Double.isNaN(volts) || Double.isInfinite(volts)) {
      throw new ArithmeticException(
          "calculate() returned " + volts + ". Is something divided by zero?");
    }
    requestedVolts = volts;
    appliedVolts = MathUtil.clamp(volts, -MAX_VOLTS, MAX_VOLTS);

    if (!sandbox) {
      trace.time.add(time());
      trace.goal.add(goal);
      trace.output.add(plant.output());
      trace.measured.add(
          plant.kind == Plant.Kind.FLYWHEEL ? mechanism.velocity() : mechanism.position());
      trace.error.add(plant.error(goal));
      trace.velocity.add(plant.velocity());
      trace.position.add(plant.position());
      trace.volts.add(appliedVolts);
      trace.estimate.add(mechanism.estimate());
      trace.addPlots(mechanism.plots());
    }

    plant.step(appliedVolts, Mechanism.DT);
    loop++;
  }

  /** Something happens to the mechanism (from the scenario, or a dashboard button). */
  void apply(Scenario.Event event) {
    switch (event.type()) {
      case SHOT -> {
        plant.shoot();
        shots++;
      }
      case PAYLOAD_ON -> plant.setPayload(true);
      case PAYLOAD_OFF -> plant.setPayload(false);
      case BUMP -> plant.bump(event.amount());
    }
  }

  /** Refreshes what the controller sees: sensors (with the scenario's noise) and the goal. */
  private void measure() {
    Scenario scenario = spec.scenario();
    double position = plant.reportedPosition() + random.nextGaussian() * scenario.positionNoise;
    if (plant.kind == Plant.Kind.STEERING) {
      position = Plant.wrapDegrees(position);
    }
    double velocity = plant.velocity() + random.nextGaussian() * scenario.velocityNoise;
    mechanism.update(goal, position, velocity, time(), appliedVolts);
  }

  /** The SysId fit of this trial's data, for challenges that ask for one. */
  SysId.Fit fit() {
    if (!fitDone) {
      fit = spec.sysId() ? SysId.fit(trace) : null;
      fitDone = true;
    }
    return fit;
  }

  /** Formats an amount in this mechanism's units for check details. */
  String format(double amount) {
    return switch (plant.kind) {
      case FLYWHEEL -> String.format("%.0f RPM", amount);
      case STEERING -> String.format("%.1f°", amount);
      case ELEVATOR -> String.format("%.1f cm", amount * 100);
      case ARM -> String.format("%.1f°", Math.toDegrees(amount));
    };
  }
}
