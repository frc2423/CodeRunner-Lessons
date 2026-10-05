package frc.robot.lab;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.config.ModuleConfig;
import com.pathplanner.lib.config.PIDConstants;
import com.pathplanner.lib.config.RobotConfig;
import com.pathplanner.lib.controllers.PPHolonomicDriveController;
import com.pathplanner.lib.util.PathPlannerLogging;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StringPublisher;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Subsystem;
import frc.robot.subsystems.Drive;
import frc.robot.subsystems.Indexer;
import frc.robot.subsystems.Intake;
import frc.robot.subsystems.Leds;
import frc.robot.subsystems.Shooter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.littletonrobotics.junction.Logger;

/**
 * The command lab: sets up the challenge the <b>Command Lab</b> dashboard asks for, runs the
 * command scheduler and the simulated robot, plays a graded trial's scripted driver, and reports
 * what happened.
 *
 * <p>The dashboard writes {@code CommandLab/RunRequest} in the SmartDashboard table, as {@code
 * "<challenge id>|<trial or free>#<a number that changes on every click>"}; an empty id means stop.
 * Every loop, this class logs the robot, the field and the scheduler under {@code CommandLab/} (so
 * the dashboard and AdvantageScope can show them), and when a trial ends it publishes the graded
 * result on {@code /CommandLab/Result}.
 *
 * <p>You don't need to edit this file.
 */
public class CommandLab {
  private static final String RUN_REQUEST = "CommandLab/RunRequest";
  private static final String VIRTUAL = "CommandLab/Virtual";
  private static final String FIELD_EVENT = "CommandLab/FieldEvent";
  private static final String[] BASE_SUBSYSTEMS = {"Drive", "Intake", "Indexer", "Shooter", "Leds"};

  /** The robot's subsystems and controller, shared by every challenge. */
  public record Parts(
      Drive drive,
      Intake intake,
      Indexer indexer,
      Shooter shooter,
      Leds leds,
      LabController controller) {}

  private static Parts parts;

  static Parts parts() {
    if (parts == null) {
      throw new IllegalStateException("The command lab hasn't made the robot's subsystems yet.");
    }
    return parts;
  }

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
    DONE("done");

    final String id;

    Phase(String id) {
      this.id = id;
    }
  }

  private final World world = Hardware.world();
  private final Parts robot;
  private final CommandTracker tracker;
  private final StringPublisher catalogPublisher;
  private final StringPublisher resultPublisher;

  private String lastRequest = "";
  private String lastFieldEvent = "";

  private Challenges.Spec spec = null;
  private Mode mode = Mode.NONE;
  private Phase phase = Phase.IDLE;
  private String runId = "";
  private String error = "";
  private String notice = "";
  private CommandChallenge challenge = null;
  private Supplier<CommandChallenge> factory = null;
  private Command autoCommand = null;
  private Script script = null;
  private TrialLog log = null;
  private int nextRollIn = 0;
  private int nextJam = 0;
  private int lastShotLogged = 0;
  private List<Pose2d> activePath = List.of();
  private TrialResult lastResult = null;

  /** A graded trial. */
  record TrialResult(String id, boolean passed, int stars, List<Check.Outcome> outcomes) {}

  public CommandLab() {
    Drive drive = new Drive();
    Intake intake = new Intake();
    Indexer indexer = new Indexer();
    Shooter shooter = new Shooter();
    Leds leds = new Leds();
    LabController controller = new LabController();
    robot = new Parts(drive, intake, indexer, shooter, leds, controller);
    parts = robot;

    tracker = new CommandTracker(world::time, world::resets);
    configurePathPlanner(drive);
    PathPlannerLogging.setLogActivePathCallback(poses -> activePath = List.copyOf(poses));

    NetworkTableInstance nt = NetworkTableInstance.getDefault();
    catalogPublisher = nt.getStringTopic("/CommandLab/Catalog").publish();
    resultPublisher = nt.getStringTopic("/CommandLab/Result").publish();
    SmartDashboard.setDefaultString(RUN_REQUEST, "");
    SmartDashboard.setDefaultString(FIELD_EVENT, "");
    SmartDashboard.setDefaultNumberArray(VIRTUAL, new double[] {0, -1, 0, 0, 0, 0, 0, 0});
    resultPublisher.set("");
    catalogPublisher.set(catalog());

    world.reset(Script.freePlay());
    resetScheduler();
  }

  private static void configurePathPlanner(Drive drive) {
    RobotConfig config =
        new RobotConfig(
            55.0,
            5.0,
            new ModuleConfig(0.048, 4.5, 1.2, DCMotor.getKrakenX60(1).withReduction(6.75), 60, 1),
            new Translation2d(0.28, 0.28),
            new Translation2d(0.28, -0.28),
            new Translation2d(-0.28, 0.28),
            new Translation2d(-0.28, -0.28));
    AutoBuilder.configure(
        drive::getPose,
        drive::resetPose,
        drive::getRobotRelativeSpeeds,
        (speeds, feedforwards) -> drive.runVelocity(speeds),
        new PPHolonomicDriveController(new PIDConstants(5.0, 0, 0), new PIDConstants(5.0, 0, 0)),
        config,
        () -> false, // this lesson always plays on the blue side
        drive);
  }

  // The robot loop ------------------------------------------------------------------------------

  /** Called every loop (about every 20 ms), in every mode. */
  public void periodic() {
    readRequests();
    updatePhase();

    if (mode == Mode.TRIAL && phase == Phase.RUNNING) {
      double time = world.time();
      if (!script.driverControlled) {
        script.apply(time, robot.controller());
      }
      while (nextRollIn < script.rollIns.size() && time >= script.rollIns.get(nextRollIn) - 1e-9) {
        world.rollInPiece();
        nextRollIn++;
      }
      while (nextJam < script.jams.size() && time >= script.jams.get(nextJam) - 1e-9) {
        world.jamNextPiece();
        nextJam++;
      }
    } else {
      robot.controller().useRealInputs();
    }

    runScheduler();
    world.step();

    if (mode == Mode.TRIAL && phase == Phase.RUNNING) {
      log.frames.add(frame());
      for (World.Shot shot : world.shots()) {
        if (shot.id() > lastShotLogged) {
          log.shots.add(shot);
          lastShotLogged = shot.id();
        }
      }
      if (world.time() >= script.duration - 1e-9) {
        finishTrial();
      }
    }

    publish();
  }

  /** Called once when the robot is enabled in autonomous. */
  public void autonomousInit() {
    if (mode == Mode.FREE && challenge != null) {
      scheduleAuto();
    }
  }

  /** Called once when the robot is enabled in teleop. */
  public void teleopInit() {
    if (autoCommand != null) {
      autoCommand.cancel();
      autoCommand = null;
    }
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
        start(id, requested, request, null);
      }
    }

    robot.controller().setVirtual(SmartDashboard.getNumberArray(VIRTUAL, new double[0]));

    String event = SmartDashboard.getString(FIELD_EVENT, "");
    if (!event.equals(lastFieldEvent)) {
      lastFieldEvent = event;
      String name = event.contains("#") ? event.substring(0, event.indexOf('#')) : event;
      boolean trialRunning = mode == Mode.TRIAL && phase != Phase.DONE;
      if (!trialRunning) {
        switch (name) {
          case "rollin" -> world.rollInPiece();
          case "reset" -> {
            if (spec != null) {
              start(spec.id(), Mode.FREE, runId, null);
            } else {
              world.reset(Script.freePlay());
            }
          }
          default -> {}
        }
      }
    }
  }

  /**
   * Sets up a challenge: clears every binding, default command and running command, puts the robot
   * and field back, and makes a fresh challenge object.
   *
   * @param override the challenge to make instead of the student's (for the calibration test)
   */
  void start(String id, Mode newMode, String request, Supplier<CommandChallenge> override) {
    stopEverything();
    error = "";
    notice = "";
    lastResult = null;
    Challenges.Spec found = Challenges.get(id);
    if (found == null) {
      error = "The robot program has no challenge called \"" + id + "\".";
      return;
    }
    spec = found;
    mode = newMode;
    runId = request;
    factory = override != null ? override : spec.factory();
    script = newMode == Mode.TRIAL ? spec.script() : freePlayScript(spec);
    world.reset(script);

    if (!makeChallenge()) {
      return;
    }
    if (newMode == Mode.TRIAL) {
      phase = Phase.WAITING;
      System.out.println("[CommandLab] Trial waiting for the robot to be enabled: " + id);
    } else {
      phase = Phase.RUNNING;
      if (spec.freeDrive()
          && CommandScheduler.getInstance().getDefaultCommand(robot.drive()) == null) {
        robot.drive().setDefaultCommand(FreePlay.drive(robot.drive(), robot.controller()));
      }
      if (DriverStation.isAutonomousEnabled()) {
        scheduleAuto();
      }
      System.out.println("[CommandLab] Free play: " + id);
    }
  }

  private static Script freePlayScript(Challenges.Spec spec) {
    Script free = Script.freePlay().start(spec.script().start);
    if (spec.script().preload) {
      free.preload();
    }
    return free;
  }

  /** Makes the challenge object and calls its configureBindings. False if that failed. */
  private boolean makeChallenge() {
    try {
      challenge = factory.get();
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
    return true;
  }

  private void updatePhase() {
    if (mode != Mode.TRIAL || script == null) {
      return;
    }
    boolean auto = script.mode == Script.Mode.AUTO;
    boolean ready =
        DriverStation.isEnabled()
            && (auto ? DriverStation.isAutonomous() : DriverStation.isTeleop());
    if (phase == Phase.WAITING && ready) {
      beginTrial();
    } else if (phase == Phase.RUNNING && !ready) {
      String wanted = script.mode.label;
      stopEverything();
      notice =
          DriverStation.isEnabled()
              ? "The trial stopped because the robot left " + wanted + " mode."
              : "The trial stopped because the robot was disabled.";
      System.out.println("[CommandLab] " + notice);
    }
  }

  /** The robot was enabled in the right mode: put everything back and start the clock. */
  private void beginTrial() {
    // Commands made while waiting might have moved something, so start from the script's setup.
    clearRobot();
    world.reset(script);
    if (!makeChallenge()) {
      return;
    }
    mode = Mode.TRIAL;
    phase = Phase.RUNNING;
    nextRollIn = 0;
    nextJam = 0;
    lastShotLogged = 0;
    log = new TrialLog();
    tracker.record(log.events);
    if (!script.driverControlled) {
      script.apply(0, robot.controller());
    }
    if (script.mode == Script.Mode.AUTO) {
      scheduleAuto();
    }
    System.out.println("[CommandLab] Trial started: " + spec.id());
  }

  private void scheduleAuto() {
    if (autoCommand != null) {
      autoCommand.cancel();
    }
    try {
      autoCommand = challenge.getAutonomousCommand();
      if (autoCommand != null) {
        CommandScheduler.getInstance().schedule(autoCommand);
      }
    } catch (RuntimeException | StackOverflowError e) {
      autoCommand = null;
      fail("getAutonomousCommand()", e);
    }
  }

  private void finishTrial() {
    tracker.stopRecording();
    TrialResult result = grade(spec, log);
    lastResult = result;
    resultPublisher.set(resultJson(result));
    phase = Phase.DONE;
    mode = Mode.FREE;
    robot.controller().useRealInputs();
    System.out.println(
        "[CommandLab] Trial finished: "
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
    System.out.println("[CommandLab] Stopped");
  }

  /** Stops the challenge: see {@link #clearRobot()}, and forget which challenge was running. */
  private void stopEverything() {
    clearRobot();
    spec = null;
    mode = Mode.NONE;
    phase = Phase.IDLE;
    runId = "";
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
    resetScheduler();
    tracker.stopRecording();
    challenge = null;
    autoCommand = null;
    robot.drive().stop();
    robot.intake().stopRollers();
    robot.intake().retract();
    robot.indexer().stop();
    robot.shooter().stop();
    Hardware.setClimberVolts(0);
    robot.controller().useRealInputs();
    robot.controller().resetRumble();
  }

  /** Removes every binding, every subsystem's default command and every subsystem but ours. */
  private void resetScheduler() {
    CommandScheduler scheduler = CommandScheduler.getInstance();
    scheduler.getDefaultButtonLoop().clear();
    scheduler.setActiveButtonLoop(scheduler.getDefaultButtonLoop());
    scheduler.unregisterAllSubsystems();
    scheduler.registerSubsystem(
        robot.drive(), robot.intake(), robot.indexer(), robot.shooter(), robot.leds());
    tracker.clearActive();
  }

  /**
   * After a command throws in the middle of {@code CommandScheduler.run()}, the scheduler is left
   * believing it's still running. Put it back in a usable state.
   */
  @SuppressWarnings("unchecked")
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
      DriverStation.reportWarning("[CommandLab] Couldn't reset the command scheduler: " + e, false);
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

  /** Every subsystem registered with the scheduler, including ones a challenge made. */
  @SuppressWarnings("unchecked")
  private List<Subsystem> registeredSubsystems() {
    try {
      Map<Subsystem, Command> map =
          (Map<Subsystem, Command>) get(CommandScheduler.getInstance(), "m_subsystems");
      return new ArrayList<>(map.keySet());
    } catch (ReflectiveOperationException | RuntimeException e) {
      return List.of(robot.drive(), robot.intake(), robot.indexer(), robot.shooter(), robot.leds());
    }
  }

  /** Stops the challenge after it throws, and reports where the problem was. */
  private void fail(String where, Throwable e) {
    String id = spec != null ? spec.id() : "";
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
    DriverStation.reportError(
        "[CommandLab] Challenge \"" + id + "\" stopped in " + where + ": " + message,
        cause.getStackTrace());
    stopEverything();
    error = message;
  }

  // What the dashboard and AdvantageScope see -----------------------------------------------

  private Frame frame() {
    LabController controller = robot.controller();
    double[] axes = new double[6];
    for (int i = 0; i < 6; i++) {
      axes[i] = controller.getRawAxis(i);
    }
    Map<String, String> owners = new LinkedHashMap<>();
    List<String> commands = new ArrayList<>();
    for (Command command : tracker.active()) {
      String name = CommandTracker.name(command);
      commands.add(name);
      for (String requirement : CommandTracker.requirements(command)) {
        owners.put(requirement, name);
      }
    }
    return new Frame(
        world.time(),
        DriverStation.isEnabled(),
        controller.buttonBits(),
        controller.getPov(),
        axes,
        world.pose(),
        Math.hypot(world.fieldVx(), world.fieldVy()),
        world.omega(),
        world.intakeDeployRequested(),
        world.deploy(),
        world.rollers(),
        world.indexer(),
        world.hasPiece(),
        world.beamBreak(),
        world.jammed(),
        world.rpm(),
        world.targetRpm(),
        robot.shooter().atSpeed(),
        world.climberHeight(),
        world.climberVolts(),
        world.hang(),
        world.strain(),
        world.led(),
        alliance(),
        world.score(),
        world.misses(),
        world.dribbles(),
        world.pickups(),
        controller.rumble(),
        owners,
        commands);
  }

  private static String alliance() {
    Optional<Alliance> alliance = DriverStation.getAlliance();
    return alliance.map(a -> a == Alliance.Red ? "red" : "blue").orElse("");
  }

  private void publish() {
    Logger.recordOutput("CommandLab/Running", spec == null ? "" : spec.id());
    Logger.recordOutput("CommandLab/Mode", mode.id);
    Logger.recordOutput("CommandLab/Phase", phase.id);
    Logger.recordOutput("CommandLab/Waiting", waitingMessage());
    Logger.recordOutput("CommandLab/Notice", notice);
    Logger.recordOutput("CommandLab/RunId", runId);
    Logger.recordOutput("CommandLab/Error", error);
    Logger.recordOutput("CommandLab/Time", world.time());
    Logger.recordOutput(
        "CommandLab/Duration", mode == Mode.TRIAL && script != null ? script.duration : 0.0);
    Logger.recordOutput("CommandLab/State", state());

    List<Translation2d> pieces = world.pieces();
    double[] flat = new double[pieces.size() * 2];
    Translation3d[] pieces3d = new Translation3d[pieces.size()];
    for (int i = 0; i < pieces.size(); i++) {
      flat[2 * i] = pieces.get(i).getX();
      flat[2 * i + 1] = pieces.get(i).getY();
      pieces3d[i] = new Translation3d(pieces.get(i).getX(), pieces.get(i).getY(), 0.075);
    }
    Logger.recordOutput("CommandLab/Pieces", flat);
    Logger.recordOutput("CommandLab/GamePieces", pieces3d);
    Logger.recordOutput("CommandLab/RobotPose", world.pose());

    List<World.Shot> shots = world.shots();
    double[] shotData = new double[shots.size() * 9];
    for (int i = 0; i < shots.size(); i++) {
      World.Shot s = shots.get(i);
      double[] one = {
        s.id(), s.x0(), s.y0(), s.x1(), s.y1(), s.launched(), s.flight(), s.outcome(), s.rpm()
      };
      System.arraycopy(one, 0, shotData, i * 9, 9);
    }
    Logger.recordOutput("CommandLab/Shots", shotData);

    LabController controller = robot.controller();
    double[] input = new double[10];
    input[0] = controller.source() == LabController.Source.SCRIPT ? 2 : 1;
    input[1] = controller.buttonBits();
    input[2] = controller.getPov();
    for (int i = 0; i < 6; i++) {
      input[3 + i] = controller.getRawAxis(i);
    }
    input[9] = controller.rumble();
    Logger.recordOutput("CommandLab/Controller", input);

    List<Command> active = tracker.active();
    String[] commands = new String[active.size()];
    for (int i = 0; i < active.size(); i++) {
      Command command = active.get(i);
      commands[i] =
          CommandTracker.name(command)
              + "\t"
              + String.join(",", CommandTracker.requirements(command));
    }
    Logger.recordOutput("CommandLab/Commands", commands);

    Set<String> seen = new LinkedHashSet<>(List.of(BASE_SUBSYSTEMS));
    List<String> subsystems = new ArrayList<>();
    CommandScheduler scheduler = CommandScheduler.getInstance();
    List<Subsystem> registered = registeredSubsystems();
    registered.sort(
        (a, b) -> order(CommandTracker.subsystemName(a)) - order(CommandTracker.subsystemName(b)));
    for (Subsystem subsystem : registered) {
      String name = CommandTracker.subsystemName(subsystem);
      seen.remove(name);
      Command current = scheduler.requiring(subsystem);
      Command fallback = scheduler.getDefaultCommand(subsystem);
      subsystems.add(
          name
              + "\t"
              + (current == null ? "" : CommandTracker.name(current))
              + "\t"
              + (fallback == null ? "" : CommandTracker.name(fallback)));
    }
    Logger.recordOutput("CommandLab/Subsystems", subsystems.toArray(new String[0]));

    List<CommandTracker.Event> recent = tracker.recent();
    String[] events = new String[recent.size()];
    for (int i = 0; i < recent.size(); i++) {
      CommandTracker.Event e = recent.get(i);
      events[i] =
          e.seq()
              + "\t"
              + e.run()
              + "\t"
              + Check.f("%.3f", e.time())
              + "\t"
              + e.type().id
              + "\t"
              + e.name()
              + "\t"
              + String.join(",", e.requirements())
              + "\t"
              + (e.by() == null ? "" : e.by());
    }
    Logger.recordOutput("CommandLab/Events", events);

    double[] path = new double[activePath.size() * 2];
    for (int i = 0; i < activePath.size(); i++) {
      path[2 * i] = activePath.get(i).getX();
      path[2 * i + 1] = activePath.get(i).getY();
    }
    Logger.recordOutput("CommandLab/Path", path);
  }

  private static int order(String subsystem) {
    for (int i = 0; i < BASE_SUBSYSTEMS.length; i++) {
      if (BASE_SUBSYSTEMS[i].equals(subsystem)) {
        return i;
      }
    }
    return BASE_SUBSYSTEMS.length;
  }

  /** What the trial is waiting for, in words, or "". */
  private String waitingMessage() {
    if (phase != Phase.WAITING || script == null) {
      return "";
    }
    String wanted = script.mode.label;
    if (!DriverStation.isEnabled()) {
      return "Set the Driver Station to " + wanted + " and click Enable to start the trial.";
    }
    return "The robot is enabled in the wrong mode. Disable it, switch the Driver Station to "
        + wanted
        + ", then Enable.";
  }

  /** The robot and field, packed in one array so each sample stays together. */
  private double[] state() {
    Pose2d pose = world.pose();
    Pose2d odometry = world.odometryPose();
    Color led = world.led();
    return new double[] {
      world.time(),
      pose.getX(),
      pose.getY(),
      pose.getRotation().getRadians(),
      world.fieldVx(),
      world.fieldVy(),
      world.omega(),
      world.deploy(),
      world.rollers(),
      world.indexer(),
      world.piece(),
      world.rpm(),
      world.targetRpm(),
      world.climberHeight(),
      world.climberVolts(),
      world.hang(),
      Math.round(MathUtil.clamp(led.red, 0, 1) * 255),
      Math.round(MathUtil.clamp(led.green, 0, 1) * 255),
      Math.round(MathUtil.clamp(led.blue, 0, 1) * 255),
      world.score(),
      world.misses(),
      world.dribbles(),
      DriverStation.isEnabled() ? 1 : 0,
      world.jammed() ? 1 : 0,
      world.rollerCurrent(),
      world.beamBreak() ? 1 : 0,
      world.hooked() ? 1 : 0,
      odometry.getX(),
      odometry.getY(),
      odometry.getRotation().getRadians(),
      world.strain(),
      robot.shooter().atSpeed() ? 1 : 0,
      world.resets()
    };
  }

  /** Every challenge's mode, length and checks, for the dashboard. */
  private static String catalog() {
    Json json = new Json().beginObject().key("challenges").beginArray();
    for (String id : Challenges.ids()) {
      Challenges.Spec spec = Challenges.get(id);
      json.beginObject()
          .key("id")
          .value(id)
          .key("mode")
          .value(spec.script().mode == Script.Mode.AUTO ? "auto" : "teleop")
          .key("duration")
          .value(spec.script().duration)
          .key("driverControlled")
          .value(spec.script().driverControlled);
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
    json.key("score").value(world.score());
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
    return json.endObject().toString();
  }

  // For the calibration test ----------------------------------------------------------------

  Phase phase() {
    return phase;
  }

  Mode mode() {
    return mode;
  }

  String error() {
    return error;
  }

  TrialResult lastResult() {
    return lastResult;
  }

  TrialLog lastLog() {
    return log;
  }
}
