package frc.robot.lab;

import edu.wpi.first.math.util.Units;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StringPublisher;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;
import org.littletonrobotics.junction.AutoLogOutputManager;
import org.littletonrobotics.junction.LogTable.LogValue;
import org.littletonrobotics.junction.Logger;

/**
 * The lab: sets up the challenge the <b>AdvantageKit Lab</b> dashboard asks for, runs the command
 * scheduler and the simulated robot, plays a graded trial's scripted driver, replays the trial's
 * log through your code, and reports what happened.
 *
 * <p>The dashboard writes {@code AKitLab/RunRequest} in the SmartDashboard table, as {@code
 * "<challenge id>|<trial or free>#<a number that changes on every click>"}; an empty id means stop.
 * The lab talks to the dashboard over plain NetworkTables topics under {@code /AKitLab/}, so your
 * log only holds what you put there.
 *
 * <p>You don't need to edit this file.
 */
public class AKitLab {
  private static final String RUN_REQUEST = "AKitLab/RunRequest";
  private static final String VIRTUAL = "AKitLab/Virtual";
  private static final String FIELD_EVENT = "AKitLab/FieldEvent";
  private static final int TREE_EVERY_N_LOOPS = 5;
  private static final int MAX_SERIES = 48;

  enum Mode {
    NONE(""),
    TRIAL("trial"),
    FREE("free");

    final String id;

    Mode(String id) {
      this.id = id;
    }
  }

  enum Phase {
    IDLE(""),
    WAITING("waiting"),
    RUNNING("running"),
    REPLAYING("replaying"),
    DONE("done");

    final String id;

    Phase(String id) {
      this.id = id;
    }
  }

  /** A graded trial. */
  record TrialResult(String id, boolean passed, int stars, List<Check.Outcome> outcomes) {}

  private static LabController controller;

  static LabController controller() {
    if (controller == null) {
      throw new IllegalStateException("The lab hasn't made the driver's controller yet.");
    }
    return controller;
  }

  private final World world = Hardware.world();
  private final boolean replaying = Logger.hasReplaySource();

  private StringPublisher catalogPublisher;
  private StringPublisher resultPublisher;
  private StringPublisher statusPublisher;
  private StringPublisher treePublisher;

  private String lastRequest = "";
  private String lastFieldEvent = "";
  private Challenges.Spec spec = null;
  private Mode mode = Mode.NONE;
  private Phase phase = Phase.IDLE;
  private String runId = "";
  private String error = "";
  private String notice = "";
  private LabChallenge challenge = null;
  private Script script = null;
  private int nextEvent = 0;
  private TrialLog log = null;
  private ReplayRunner replay = null;
  private long loops = 0;

  // In a replay: the challenge to run, and whether it's been made yet.
  private String replayId = "";
  private boolean replayStarted = false;

  public AKitLab() {
    controller = new LabController();
    if (replaying) {
      Hardware.enterReplay();
      startReplayWatchdog();
      String requested = System.getenv(ReplayRunner.CHALLENGE_VARIABLE);
      replayId = requested == null ? "" : requested;
      return;
    }

    NetworkTableInstance nt = NetworkTableInstance.getDefault();
    catalogPublisher = nt.getStringTopic("/AKitLab/Catalog").publish();
    resultPublisher = nt.getStringTopic("/AKitLab/Result").publish();
    statusPublisher = nt.getStringTopic("/AKitLab/Status").publish();
    treePublisher = nt.getStringTopic("/AKitLab/Tree").publish();
    SmartDashboard.setDefaultString(RUN_REQUEST, "");
    SmartDashboard.setDefaultString(FIELD_EVENT, "");
    SmartDashboard.setDefaultNumberArray(VIRTUAL, new double[] {0, 0, 0, 0, 0, 0, 0});
    resultPublisher.set("");
    catalogPublisher.set(catalog());
    world.reset(new Script(0).start);
  }

  // The robot loop ------------------------------------------------------------------------------

  /** Called every loop (about every 20 ms), in every mode. */
  public void periodic() {
    if (replaying) {
      replayPeriodic();
      return;
    }
    loops++;
    readRequests();
    updatePhase();

    boolean trialRunning = mode == Mode.TRIAL && phase == Phase.RUNNING;
    if (trialRunning) {
      double time = world.time();
      script.apply(time, controller);
      while (nextEvent < script.events.size()
          && time >= script.events.get(nextEvent).time() - 1e-9) {
        script.events.get(nextEvent).action().run();
        nextEvent++;
      }
    } else {
      controller.useRealInputs();
    }

    controller.update();
    runScheduler();

    // A crash in the scheduler ends the trial, so look again.
    if (mode == Mode.TRIAL && phase == Phase.RUNNING) {
      log.frames.add(new Frame(world.time(), LogAccess.snapshot(), world.state()));
    }
    world.step();

    if (mode == Mode.TRIAL && phase == Phase.RUNNING) {
      log.shots.clear();
      log.shots.addAll(world.shots());
      if (world.time() >= script.duration - 1e-9) {
        finishTrial();
      }
    }
    pollReplay();
    publish();
  }

  private void runScheduler() {
    try {
      CommandScheduler.getInstance().run();
    } catch (RuntimeException | StackOverflowError e) {
      rescueScheduler();
      fail("a command", e);
    }
  }

  // Requests from the dashboard -------------------------------------------------------------

  private void readRequests() {
    String request = SmartDashboard.getString(RUN_REQUEST, "");
    if (!request.equals(lastRequest)) {
      lastRequest = request;
      int hash = request.lastIndexOf('#');
      String command = hash >= 0 ? request.substring(0, hash) : request;
      int bar = command.indexOf('|');
      String id = bar >= 0 ? command.substring(0, bar) : command;
      Mode requested =
          bar >= 0 && command.substring(bar + 1).equals("free") ? Mode.FREE : Mode.TRIAL;
      if (id.isEmpty()) {
        stop();
      } else {
        start(id, requested, request);
      }
    }

    controller.setVirtual(SmartDashboard.getNumberArray(VIRTUAL, new double[0]));

    String event = SmartDashboard.getString(FIELD_EVENT, "");
    if (!event.equals(lastFieldEvent)) {
      lastFieldEvent = event;
      String name = event.contains("#") ? event.substring(0, event.indexOf('#')) : event;
      if (name.equals("piece") && mode == Mode.FREE) {
        world.loadPiece();
      }
    }
  }

  /** Sets up a challenge: clears everything the last one left, and makes a fresh one. */
  private void start(String id, Mode newMode, String request) {
    stopEverything();
    error = "";
    notice = "";
    Challenges.Spec found = Challenges.get(id);
    if (found == null) {
      error = "The robot program has no challenge called \"" + id + "\".";
      return;
    }
    spec = found;
    mode = newMode;
    runId = request;
    script = spec.script();
    world.reset(script.start);
    Challenges.prepare(spec);

    if (spec.replayLog() != null && newMode == Mode.TRIAL) {
      phase = Phase.REPLAYING;
      log = new TrialLog();
      startReplay(
          Filesystem.getDeployDirectory().toPath().resolve("match-logs").resolve(spec.replayLog()),
          null);
      System.out.println("[AKitLab] Replaying " + spec.replayLog() + " for: " + id);
      return;
    }
    if (!makeChallenge()) {
      return;
    }
    if (newMode == Mode.TRIAL) {
      phase = Phase.WAITING;
      System.out.println("[AKitLab] Trial waiting for the robot to be enabled: " + id);
    } else {
      phase = Phase.RUNNING;
      System.out.println("[AKitLab] Free play: " + id);
    }
  }

  /** Makes the challenge object and calls its configureBindings. False if that failed. */
  private boolean makeChallenge() {
    LogAccess.clear();
    LogAccess.resetAutoLog();
    LogAccess.resetDashboardInputs();
    try {
      challenge = spec.factory().get();
    } catch (RuntimeException | StackOverflowError | ExceptionInInitializerError e) {
      fail("its constructor", e);
      return false;
    }
    try {
      challenge.configureBindings();
    } catch (RuntimeException | StackOverflowError e) {
      fail("configureBindings()", e);
      return false;
    }
    AutoLogOutputManager.addObject(challenge);
    return true;
  }

  private void updatePhase() {
    if (mode != Mode.TRIAL || script == null) {
      return;
    }
    boolean ready = DriverStation.isEnabled() && DriverStation.isTeleop();
    if (phase == Phase.WAITING && ready) {
      beginTrial();
    } else if (phase == Phase.RUNNING && !ready) {
      LabRecorder.get().cancel();
      String why =
          DriverStation.isEnabled()
              ? "The trial stopped because the robot left Teleop mode."
              : "The trial stopped because the robot was disabled.";
      stopEverything();
      notice = why;
      System.out.println("[AKitLab] " + notice);
    }
  }

  /** The robot was enabled in Teleop: put everything back and start the clock. */
  private void beginTrial() {
    clearRobot();
    world.reset(script.start);
    Challenges.prepare(spec);
    if (!makeChallenge()) {
      return;
    }
    phase = Phase.RUNNING;
    nextEvent = 0;
    log = new TrialLog();
    if (spec.replays()) {
      LabRecorder.get().begin(ReplayRunner.trialLogPath(), Logger.getTimestamp());
    }
    System.out.println("[AKitLab] Trial started: " + spec.id());
  }

  private void finishTrial() {
    System.out.println("[AKitLab] Trial finished: " + spec.id());
    controller.useRealInputs();
    if (spec.replays()) {
      LabRecorder.get().end(Logger.getTimestamp());
      phase = Phase.REPLAYING;
      startReplay(Path.of(ReplayRunner.trialLogPath()), LabRecorder.get());
    } else {
      publishResult();
    }
  }

  private void startReplay(Path file, LabRecorder recorder) {
    replay = new ReplayRunner();
    replay.start(file, spec.id(), recorder);
  }

  private void pollReplay() {
    if (phase != Phase.REPLAYING || replay == null || !replay.finished()) {
      return;
    }
    ReplayRunner.Outcome outcome = replay.outcome();
    replay = null;
    log.replay = outcome.log();
    log.replayError = outcome.error();
    publishResult();
  }

  /** Grades the finished trial and sends the result to the dashboard. */
  private void publishResult() {
    TrialResult result = grade(spec, log);
    resultPublisher.set(resultJson(result));
    phase = Phase.DONE;
    mode = Mode.FREE;
    System.out.println(
        "[AKitLab] Graded: "
            + spec.id()
            + (result.passed() ? " (passed" : " (not passed")
            + ", "
            + result.stars()
            + " bonus)");
  }

  static TrialResult grade(Challenges.Spec spec, TrialLog log) {
    boolean passed = true;
    int stars = 0;
    List<Check.Outcome> outcomes = new ArrayList<>();
    for (Check check : spec.checks()) {
      Check.Outcome outcome = check.evaluate(log);
      outcomes.add(outcome);
      if (check.bonus) {
        stars += outcome.pass() ? 1 : 0;
      } else {
        passed &= outcome.pass();
      }
    }
    return new TrialResult(spec.id(), passed, stars, outcomes);
  }

  private void stop() {
    stopEverything();
    error = "";
    notice = "";
    System.out.println("[AKitLab] Stopped");
  }

  /** Stops the challenge: see {@link #clearRobot()}, and forget which challenge was running. */
  private void stopEverything() {
    LabRecorder.get().cancel();
    if (replay != null) {
      replay.abandon();
      replay = null;
    }
    clearRobot();
    spec = null;
    mode = Mode.NONE;
    phase = Phase.IDLE;
    runId = "";
    script = null;
  }

  /** Cancels every command, forgets every binding and default command, and stops the motors. */
  private void clearRobot() {
    CommandScheduler scheduler = CommandScheduler.getInstance();
    try {
      scheduler.cancelAll();
    } catch (RuntimeException | StackOverflowError e) {
      // A command's end() threw. Throw it all away anyway.
      rescueScheduler();
    }
    scheduler.getDefaultButtonLoop().clear();
    scheduler.setActiveButtonLoop(scheduler.getDefaultButtonLoop());
    scheduler.unregisterAllSubsystems();
    clearSchedulerCallbacks();
    challenge = null;
    controller.useRealInputs();
    world.setFlywheelVolts(0);
    world.setFeederVolts(0);
    world.setSpeeds(0, 0, 0);
    world.setPivotVolts(0);
  }

  /** Removes the callbacks a challenge registered with {@code onCommandInitialize} and friends. */
  private static void clearSchedulerCallbacks() {
    CommandScheduler scheduler = CommandScheduler.getInstance();
    try {
      for (String name :
          List.of("m_initActions", "m_executeActions", "m_interruptActions", "m_finishActions")) {
        ((Collection<?>) get(scheduler, name)).clear();
      }
    } catch (ReflectiveOperationException | RuntimeException e) {
      DriverStation.reportWarning(
          "[AKitLab] Couldn't reset the scheduler's callbacks: " + e, false);
    }
  }

  /**
   * After a command throws in the middle of {@code CommandScheduler.run()}, the scheduler is left
   * believing it's still running. Put it back in a usable state.
   */
  private static void rescueScheduler() {
    CommandScheduler scheduler = CommandScheduler.getInstance();
    try {
      set(scheduler, "m_inRunLoop", false);
      ((Collection<?>) get(scheduler, "m_toSchedule")).clear();
      ((Collection<?>) get(scheduler, "m_toCancelCommands")).clear();
      ((Collection<?>) get(scheduler, "m_toCancelInterruptors")).clear();
      ((Collection<?>) get(scheduler, "m_endingCommands")).clear();
      ((Collection<?>) get(scheduler, "m_scheduledCommands")).clear();
      ((Map<?, ?>) get(scheduler, "m_requirements")).clear();
    } catch (ReflectiveOperationException | RuntimeException e) {
      DriverStation.reportWarning("[AKitLab] Couldn't reset the command scheduler: " + e, false);
    }
  }

  private static Object get(Object target, String name) throws ReflectiveOperationException {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private static void set(Object target, String name, Object value)
      throws ReflectiveOperationException {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  /** Stops the challenge after it throws, and reports where the problem was. */
  private void fail(String where, Throwable e) {
    String id = spec != null ? spec.id() : "";
    String message = describeCrash(e);
    Throwable cause =
        e instanceof ExceptionInInitializerError && e.getCause() != null ? e.getCause() : e;
    DriverStation.reportError(
        "[AKitLab] Challenge \"" + id + "\" stopped in " + where + ": " + message,
        cause.getStackTrace());
    stopEverything();
    error = message;
  }

  /** What went wrong, and which line of the student's code it happened on. */
  private static String describeCrash(Throwable e) {
    String message = e.getClass().getSimpleName();
    if (e.getMessage() != null) {
      message += ": " + e.getMessage();
    }
    Throwable cause =
        e instanceof ExceptionInInitializerError && e.getCause() != null ? e.getCause() : e;
    for (StackTraceElement frame : cause.getStackTrace()) {
      String name = frame.getClassName();
      if (name.startsWith("frc.robot.") && !name.startsWith("frc.robot.lab.")) {
        message += " (" + frame.getFileName() + ", line " + frame.getLineNumber() + ")";
        break;
      }
    }
    return message;
  }

  // The replaying copy of this program ----------------------------------------------------------

  /**
   * In a replay, the lab only does what's left of a real robot program: reads the logged controller
   * inputs and runs the scheduler. There's no simulated robot, no dashboard and no script.
   */
  private void replayPeriodic() {
    if (!replayStarted) {
      replayStarted = true;
      startReplayedChallenge();
    }
    controller.update();
    try {
      CommandScheduler.getInstance().run();
    } catch (RuntimeException | StackOverflowError e) {
      rescueScheduler();
      Logger.recordOutput("Lab/ReplayError", describeCrash(e));
    }
    Logger.recordOutput("Lab/HardwareTouches", Hardware.touches());
    Logger.recordOutput("Lab/FirstTouch", Hardware.firstTouch());
  }

  private void startReplayedChallenge() {
    Challenges.Spec found = Challenges.get(replayId);
    if (found == null) {
      Logger.recordOutput("Lab/ReplayError", "No challenge called \"" + replayId + "\"");
      return;
    }
    try {
      LabChallenge made = found.factory().get();
      made.configureBindings();
      AutoLogOutputManager.addObject(made);
      challenge = made;
    } catch (RuntimeException | StackOverflowError | ExceptionInInitializerError e) {
      Logger.recordOutput("Lab/ReplayError", describeCrash(e));
    }
  }

  /** A replay that's stuck, or whose robot program has gone away, shouldn't run forever. */
  private static void startReplayWatchdog() {
    Thread watchdog =
        new Thread(
            () -> {
              try {
                Thread.sleep((ReplayRunner.TIMEOUT_SECONDS + 5) * 1000);
              } catch (InterruptedException e) {
                return;
              }
              Runtime.getRuntime().halt(3);
            },
            "AKitLab replay watchdog");
    watchdog.setDaemon(true);
    watchdog.start();
  }

  // What the dashboard sees -----------------------------------------------------------------

  private void publish() {
    Json status = new Json().beginObject();
    status.key("running").value(spec == null ? "" : spec.id());
    status.key("mode").value(mode.id);
    status.key("phase").value(phase.id);
    status.key("notice").value(notice);
    status.key("runId").value(runId);
    status.key("error").value(error);
    status.key("loops").value(loops);
    status.key("time").value(world.time());
    status.key("duration").value(script != null ? script.duration : 0.0);
    status.key("scripted").value(controller.isScripted());
    status.key("buttons").value(controller.buttonBits());
    status.key("hasPiece").value(world.hasPiece());
    status.key("shots").value(world.shots().size());
    status.key("rpm").value(world.flywheelRpm());
    status.key("yawDeg").value(Units.radiansToDegrees(world.yawRad()));
    status.key("pivotDeg").value(Units.radiansToDegrees(world.pivotRad()));
    statusPublisher.set(status.endObject().toString());

    if (loops % TREE_EVERY_N_LOOPS == 0) {
      treePublisher.set(tree());
    }
  }

  /** Every value the student's code has logged, for the log view. */
  private String tree() {
    Map<String, LogValue> values = new TreeMap<>(LogAccess.snapshot());
    Json json = new Json().beginObject();
    json.key("time").value(world.time());
    json.key("items").beginArray();
    for (Map.Entry<String, LogValue> entry : values.entrySet()) {
      LogValue value = entry.getValue();
      json.beginObject();
      json.key("key").value(LogAccess.shortKey(entry.getKey()));
      json.key("kind").value(LogAccess.kind(entry.getKey()));
      json.key("type").value(LogAccess.typeName(value));
      if (value.unitStr != null) {
        json.key("unit").value(value.unitStr);
      }
      json.key("value");
      LogAccess.write(json, value);
      json.endObject();
    }
    return json.endArray().endObject().toString();
  }

  /** Every challenge's checks and shape, for the dashboard. */
  private static String catalog() {
    Json json = new Json().beginObject().key("challenges").beginArray();
    for (String id : Challenges.ids()) {
      Challenges.Spec spec = Challenges.get(id);
      json.beginObject();
      json.key("id").value(id);
      json.key("duration").value(spec.script().duration);
      json.key("replays").value(spec.replays());
      json.key("logOnly").value(spec.replayLog() != null);
      json.key("checks").beginArray();
      for (Check check : spec.checks()) {
        json.beginObject()
            .key("label")
            .value(check.label)
            .key("bonus")
            .value(check.bonus)
            .endObject();
      }
      json.endArray().endObject();
    }
    return json.endArray().endObject().toString();
  }

  private String resultJson(TrialResult result) {
    Json json = new Json().beginObject();
    json.key("id").value(result.id()).key("runId").value(runId);
    json.key("passed").value(result.passed()).key("stars").value(result.stars());
    json.key("checks").beginArray();
    List<Check> checks = spec.checks();
    for (int i = 0; i < checks.size(); i++) {
      Check check = checks.get(i);
      Check.Outcome outcome = result.outcomes().get(i);
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

    json.key("trace").beginObject();
    writeTrace(json, log);
    json.endObject();

    if (log.replay != null) {
      writeReplay(json, log.replay);
    } else if (!log.replayError.isEmpty()) {
      json.key("replayError").value(log.replayError);
    }
    return json.endObject().toString();
  }

  /** Every number and true/false the trial logged, as [time, value] pairs when it changed. */
  private static void writeTrace(Json json, TrialLog log) {
    Map<String, List<double[]>> series = new LinkedHashMap<>();
    Map<String, Double> last = new LinkedHashMap<>();
    for (Frame frame : log.frames) {
      for (Map.Entry<String, LogValue> entry : frame.log().entrySet()) {
        Double number = LogAccess.number(entry.getValue());
        if (number == null) {
          continue;
        }
        // The kind keeps an input and an output with the same name apart.
        String key = LogAccess.kind(entry.getKey()) + ":" + LogAccess.shortKey(entry.getKey());
        if (!series.containsKey(key) && series.size() >= MAX_SERIES) {
          continue;
        }
        Double previous = last.get(key);
        if (previous == null || Math.abs(previous - number) > 1e-9) {
          series
              .computeIfAbsent(key, k -> new ArrayList<>())
              .add(new double[] {frame.time(), number});
          last.put(key, number);
        }
      }
    }
    for (Map.Entry<String, List<double[]>> entry : series.entrySet()) {
      json.key(entry.getKey()).beginArray();
      for (double[] point : entry.getValue()) {
        json.value(point[0]).value(point[1]);
      }
      json.endArray();
    }
    writeTruth(json, "truth:rpm", log, frame -> frame.world().flywheelRpm());
    writeTruth(json, "truth:yawDeg", log, frame -> Units.radiansToDegrees(frame.world().yawRad()));
    writeTruth(
        json, "truth:pivotDeg", log, frame -> Units.radiansToDegrees(frame.world().pivotRad()));
  }

  /** What the robot was really doing, so the dashboard can draw it next to what was logged. */
  private static void writeTruth(
      Json json, String name, TrialLog log, ToDoubleFunction<Frame> value) {
    json.key(name).beginArray();
    double last = Double.NaN;
    for (Frame frame : log.frames) {
      double number = value.applyAsDouble(frame);
      if (Double.isNaN(last) || Math.abs(number - last) > 1e-9) {
        json.value(frame.time()).value(number);
        last = number;
      }
    }
    json.endArray();
  }

  private static void writeReplay(Json json, ReplayLog replay) {
    json.key("replay").beginObject();
    json.key("duration").value(replay.duration());
    json.key("cycles").value(replay.cycleCount());
    json.key("touches").value(replay.labNumber("HardwareTouches"));
    json.key("firstTouch").value(replay.labText("FirstTouch"));
    json.key("crash").value(replay.labText("ReplayError"));

    List<ReplayLog.Difference> differences = replay.differences();
    json.key("differences").beginArray();
    for (ReplayLog.Difference difference : differences) {
      json.beginObject()
          .key("key")
          .value(difference.key())
          .key("time")
          .value(difference.time())
          .key("real")
          .value(difference.real())
          .key("replay")
          .value(difference.replay())
          .key("cycles")
          .value(difference.cycles())
          .endObject();
    }
    json.endArray();

    List<String> keys = new ArrayList<>(replay.realKeys());
    for (String key : replay.replayKeys()) {
      if (!keys.contains(key)) {
        keys.add(key);
      }
    }
    json.key("series").beginObject();
    int count = 0;
    for (String key : keys) {
      if (count++ >= MAX_SERIES) {
        break;
      }
      json.key(key).beginObject();
      ReplayLog.Series real = replay.real(key);
      ReplayLog.Series again = replay.replay(key);
      writeSeries(json, "real", real, replay);
      writeSeries(json, "replay", again, replay);
      json.endObject();
    }
    json.endObject();

    json.key("inputs").beginObject();
    count = 0;
    for (String key : replay.inputKeys()) {
      if (count++ >= MAX_SERIES) {
        break;
      }
      writeSeries(json, key, replay.input("/" + key), replay);
    }
    json.endObject();
    json.endObject();
  }

  /** One run's values of a key, when they can be drawn. */
  private static void writeSeries(
      Json json, String name, ReplayLog.Series series, ReplayLog replay) {
    if (series == null) {
      return;
    }
    List<String> labels = new ArrayList<>();
    json.key(name).beginArray();
    for (Map.Entry<Long, Object> entry : series.values().entrySet()) {
      Object value = entry.getValue();
      double number;
      if (value instanceof Double d) {
        number = d;
      } else if (value instanceof Boolean b) {
        number = b ? 1 : 0;
      } else if (value instanceof String s) {
        int index = labels.indexOf(s);
        if (index < 0) {
          labels.add(s);
          index = labels.size() - 1;
        }
        number = index;
      } else {
        continue;
      }
      json.value(replay.seconds(entry.getKey())).value(number);
    }
    json.endArray();
    if (!labels.isEmpty()) {
      json.key(name + "Labels").beginArray();
      for (String label : labels) {
        json.value(label);
      }
      json.endArray();
    }
  }
}
