package frc.robot.lab;

import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StringPublisher;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import java.util.List;
import java.util.Map;
import org.littletonrobotics.junction.Logger;

/**
 * The control lab: runs the challenge the <b>Control Lab</b> dashboard asks for on a simulated
 * mechanism, and reports what happened.
 *
 * <p>The dashboard writes {@code ControlLab/RunRequest} in the SmartDashboard table, as {@code
 * "<challenge id>|<trial or sandbox>#<a number that changes on every click>"}; an empty id means
 * stop. Every loop, this class logs the mechanism's state under {@code ControlLab/} (so the
 * dashboard and AdvantageScope can show it), and when a trial ends it publishes the graded result
 * on {@code /ControlLab/Result}.
 *
 * <p>You don't need to edit this file.
 */
public class ControlLab {
  private static final String RUN_REQUEST = "ControlLab/RunRequest";
  private static final String SANDBOX_GOAL = "ControlLab/Sandbox/Goal";
  private static final String SANDBOX_EVENT = "ControlLab/Sandbox/Event";

  private final StringPublisher catalogPublisher;
  private final StringPublisher resultPublisher;

  private String lastRequest = "";
  private String lastEvent = "";
  private String runningId = "";
  private String runId = "";
  private Trial trial = null;
  private Trial lastTrial = null;
  private String error = "";

  public ControlLab() {
    NetworkTableInstance nt = NetworkTableInstance.getDefault();
    catalogPublisher = nt.getStringTopic("/ControlLab/Catalog").publish();
    resultPublisher = nt.getStringTopic("/ControlLab/Result").publish();
    SmartDashboard.setDefaultString(RUN_REQUEST, "");
    SmartDashboard.setDefaultString(SANDBOX_EVENT, "");
    SmartDashboard.setDefaultNumber(SANDBOX_GOAL, 0.0);
    resultPublisher.set("");
    catalogPublisher.set(catalog());
  }

  /** Called every loop (about every 20 ms), enabled or not. */
  public void periodic() {
    String request = SmartDashboard.getString(RUN_REQUEST, "");
    if (!request.equals(lastRequest)) {
      lastRequest = request;
      int hash = request.lastIndexOf('#');
      String command = hash >= 0 ? request.substring(0, hash) : request;
      int bar = command.indexOf('|');
      String id = bar >= 0 ? command.substring(0, bar) : command;
      boolean sandbox = bar >= 0 && command.substring(bar + 1).equals("sandbox");
      start(id, sandbox, request);
    }

    if (trial != null) {
      if (trial.sandbox) {
        trial.setGoal(SmartDashboard.getNumber(SANDBOX_GOAL, trial.goal()));
        String event = SmartDashboard.getString(SANDBOX_EVENT, "");
        if (!event.equals(lastEvent)) {
          lastEvent = event;
          sandboxEvent(event.contains("#") ? event.substring(0, event.indexOf('#')) : event);
        }
      }
      try {
        trial.step();
      } catch (RuntimeException | StackOverflowError e) {
        fail("calculate()", e);
      }
      if (trial != null && trial.finished()) {
        finish();
      }
    }

    log();
  }

  /** Starts a trial or sandbox of the challenge with this id, or stops if the id is empty. */
  private void start(String id, boolean sandbox, String request) {
    trial = null;
    runningId = "";
    error = "";
    if (id.isEmpty()) {
      return;
    }
    Challenges.Spec spec = Challenges.get(id);
    if (spec == null) {
      error = "The robot program has no challenge called \"" + id + "\".";
      return;
    }

    runningId = id;
    runId = request;
    Controller controller;
    try {
      controller = Challenges.create(spec);
    } catch (RuntimeException | StackOverflowError e) {
      // A field initializer or constructor in the challenge threw.
      fail("its constructor", e);
      return;
    }

    trial = new Trial(spec, controller, sandbox);
    lastTrial = trial;
    lastEvent = SmartDashboard.getString(SANDBOX_EVENT, "");
    if (sandbox) {
      SmartDashboard.putNumber(SANDBOX_GOAL, trial.goal());
    }
    System.out.println("[ControlLab] " + (sandbox ? "Sandbox" : "Trial") + " started: " + id);
    try {
      trial.start();
    } catch (RuntimeException | StackOverflowError e) {
      fail("reset()", e);
    }
  }

  private void sandboxEvent(String event) {
    Plant plant = trial.plant;
    switch (event) {
      case "shot" -> trial.apply(new Scenario.Event(0, Scenario.EventType.SHOT, 0));
      case "payload" -> trial.apply(
          new Scenario.Event(
              0,
              plant.hasPayload() ? Scenario.EventType.PAYLOAD_OFF : Scenario.EventType.PAYLOAD_ON,
              0));
      case "bump" -> {
        // A shove worth about a third of the mechanism's top speed.
        double shove = 0.35 * 12.0 / plant.kV;
        trial.apply(new Scenario.Event(0, Scenario.EventType.BUMP, shove));
      }
      default -> {}
    }
  }

  /** Grades the finished trial and sends the result to the dashboard. */
  private void finish() {
    Trial done = trial;
    trial = null;
    runningId = "";
    resultPublisher.set(result(done));
    System.out.println("[ControlLab] Trial finished: " + done.spec.id());
  }

  /** Stops the challenge after it throws, and reports where the problem was. */
  private void fail(String where, Throwable e) {
    String message = e.getClass().getSimpleName();
    if (e.getMessage() != null) {
      message += ": " + e.getMessage();
    }
    for (StackTraceElement frame : e.getStackTrace()) {
      if (frame.getClassName().startsWith("frc.robot.challenges.")) {
        message += " (" + frame.getFileName() + ", line " + frame.getLineNumber() + ")";
        break;
      }
    }
    error = message;
    DriverStation.reportError(
        "[ControlLab] Challenge \"" + runningId + "\" stopped in " + where + ": " + message,
        e.getStackTrace());
    trial = null;
    runningId = "";
  }

  private void log() {
    Trial shown = trial != null ? trial : lastTrial;
    Logger.recordOutput("ControlLab/Running", runningId);
    Logger.recordOutput(
        "ControlLab/Mode", trial == null ? "" : trial.sandbox ? "sandbox" : "trial");
    Logger.recordOutput("ControlLab/RunId", trial == null ? "" : runId);
    Logger.recordOutput("ControlLab/Error", error);
    if (shown == null) {
      return;
    }
    Plant plant = shown.plant;
    Mechanism m = shown.mechanism;
    double measured = plant.kind == Plant.Kind.FLYWHEEL ? m.velocity() : m.position();
    Logger.recordOutput("ControlLab/Mechanism", plant.kind.id);
    Logger.recordOutput("ControlLab/Time", shown.time());
    Logger.recordOutput("ControlLab/Goal", shown.goal());
    Logger.recordOutput("ControlLab/Output", plant.output());
    Logger.recordOutput("ControlLab/Measured", measured);
    Logger.recordOutput("ControlLab/Volts", shown.appliedVolts());
    Logger.recordOutput("ControlLab/RequestedVolts", shown.requestedVolts());
    Logger.recordOutput("ControlLab/Position", plant.reportedPosition());
    Logger.recordOutput("ControlLab/Velocity", plant.velocity());
    Logger.recordOutput("ControlLab/Payload", plant.hasPayload());
    Logger.recordOutput("ControlLab/Shots", shown.shots());
    // One array with everything the dashboard plots, so each sample stays together: eight fixed
    // values, then the controller's own plot lines in the order of PlotNames.
    String[] plotNames = m.plots().keySet().toArray(new String[0]);
    double[] sample = new double[8 + plotNames.length];
    sample[0] = shown.time();
    sample[1] = shown.goal();
    sample[2] = plant.output();
    sample[3] = measured;
    sample[4] = shown.appliedVolts();
    sample[5] = m.estimate();
    sample[6] = plant.reportedPosition();
    sample[7] = plant.velocity();
    for (int i = 0; i < plotNames.length; i++) {
      double value = m.plots().get(plotNames[i]);
      sample[8 + i] = value;
      Logger.recordOutput("ControlLab/Plot/" + plotNames[i], value);
    }
    Logger.recordOutput("ControlLab/PlotNames", plotNames);
    Logger.recordOutput("ControlLab/Sample", sample);
  }

  /** Every challenge's mechanism, checks and tunable numbers, for the dashboard. */
  private static String catalog() {
    Json json = new Json().beginObject().key("challenges").beginArray();
    for (String id : Challenges.ids()) {
      Challenges.Spec spec = Challenges.get(id);
      Plant plant = spec.plant().get();
      // Making each controller once registers its tunable numbers, so the dashboard can show
      // their sliders before the first run.
      String problem = "";
      try {
        Challenges.create(spec);
      } catch (RuntimeException | StackOverflowError e) {
        problem = e.toString();
      }
      json.beginObject()
          .key("id")
          .value(id)
          .key("mechanism")
          .value(plant.kind.id)
          .key("units")
          .value(plant.kind.units)
          .key("duration")
          .value(spec.scenario().duration)
          .key("problem")
          .value(problem);
      json.key("checks").beginArray();
      for (Check check : spec.checks()) {
        json.beginObject()
            .key("label")
            .value(check.label)
            .key("bonus")
            .value(check.bonus)
            .endObject();
      }
      json.endArray();
      json.key("tunables").beginArray();
      for (Map.Entry<String, Double> tunable : TunableNumber.registered(id)) {
        json.beginObject()
            .key("name")
            .value(tunable.getKey())
            .key("default")
            .value(tunable.getValue())
            .endObject();
      }
      json.endArray().endObject();
    }
    return json.endArray().endObject().toString();
  }

  /** The graded result of a finished trial, with its full trace for the dashboard's plot. */
  private String result(Trial done) {
    Json json = new Json().beginObject();
    json.key("id").value(done.spec.id()).key("runId").value(runId);

    List<Check> checks = done.spec.checks();
    boolean passed = true;
    int stars = 0;
    json.key("checks").beginArray();
    for (Check check : checks) {
      Check.Outcome outcome = check.evaluate(done);
      if (check.bonus) {
        stars += outcome.pass() ? 1 : 0;
      } else {
        passed &= outcome.pass();
      }
      json.beginObject()
          .key("label")
          .value(check.label)
          .key("bonus")
          .value(check.bonus)
          .key("pass")
          .value(outcome.pass())
          .key("detail")
          .value(outcome.detail())
          .endObject();
    }
    json.endArray();
    json.key("passed").value(passed).key("stars").value(stars);

    SysId.Fit fit = done.fit();
    if (fit != null) {
      json.key("fit")
          .beginObject()
          .key("ok")
          .value(fit.ok())
          .key("problem")
          .value(fit.problem())
          .key("kS")
          .value(fit.kS())
          .key("kV")
          .value(fit.kV())
          .key("kA")
          .value(fit.kA())
          .key("samples")
          .value(fit.samples())
          .endObject();
    }

    Trace trace = done.trace;
    json.key("trace").beginObject();
    json.key("time").array(trace.time.toArray());
    json.key("goal").array(trace.goal.toArray());
    json.key("output").array(trace.output.toArray());
    json.key("measured").array(trace.measured.toArray());
    json.key("volts").array(trace.volts.toArray());
    json.key("estimate").array(trace.estimate.toArray());
    json.key("plots").beginObject();
    for (Map.Entry<String, Trace.Series> plot : trace.plots.entrySet()) {
      json.key(plot.getKey()).array(plot.getValue().toArray());
    }
    json.endObject().endObject();
    return json.endObject().toString();
  }
}
