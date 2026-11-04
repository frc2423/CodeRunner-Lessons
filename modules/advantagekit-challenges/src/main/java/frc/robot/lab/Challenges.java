package frc.robot.lab;

import static frc.robot.lab.Check.all;
import static frc.robot.lab.Check.always;
import static frc.robot.lab.Check.becomes;
import static frc.robot.lab.Check.describeInput;
import static frc.robot.lab.Check.describeOutput;
import static frc.robot.lab.Check.f;
import static frc.robot.lab.Check.fail;
import static frc.robot.lab.Check.noHardwareInReplay;
import static frc.robot.lab.Check.of;
import static frc.robot.lab.Check.outputIs;
import static frc.robot.lab.Check.pass;
import static frc.robot.lab.Check.replayMatches;
import static frc.robot.lab.Script.A;
import static frc.robot.lab.Script.B;
import static frc.robot.lab.Script.LEFT_X;
import static frc.robot.lab.Script.LEFT_Y;
import static frc.robot.lab.Script.RIGHT_X;
import static frc.robot.lab.Script.X;
import static frc.robot.lab.Script.Y;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.networktables.NetworkTableInstance;
import frc.robot.Constants;
import frc.robot.challenges.*;
import frc.robot.subsystems.flywheel.FlywheelIO;
import frc.robot.subsystems.flywheel.FlywheelIOSim;
import frc.robot.subsystems.flywheel.FlywheelIOSpark;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.LogTable.LogValue;
import org.littletonrobotics.junction.LogTable.LoggableType;

/**
 * Every challenge the dashboard can run: its class, the trial's script (when pieces arrive, and
 * what the scripted driver presses when), and what the trial is graded on.
 *
 * <p>The ids must match the dashboard's list of challenges. You don't need to edit this file.
 */
public final class Challenges {
  /**
   * @param replays replay the trial through the student's code after it ends
   * @param replayLog replay this shipped log instead of running a trial, or null
   * @param prepare runs before each trial and free play starts, or null
   */
  record Spec(
      String id,
      Supplier<LabChallenge> factory,
      Script script,
      List<Check> checks,
      boolean replays,
      String replayLog,
      Runnable prepare) {}

  private static final Map<String, Spec> SPECS = new LinkedHashMap<>();

  private static final double SHOT_RPM = 3000;
  private static final String REPLAY_MATCHES = "The replay matches the real run";
  private static final String NO_HARDWARE = "The replay never touches the hardware";

  /** The logs the lab ships for the replay-only challenges. */
  static final String PRACTICE_MATCH = "practice-match.wpilog";

  static final String QUAL_17 = "qual-17.wpilog";

  /** How close to the true heading the heading challenge has to be, in degrees. */
  private static final double HEADING_TOLERANCE_DEG = 1.5;

  static {
    trial(
        "example",
        Challenge00Example::new,
        new Script(3.5).tap(A, 0.5).tap(A, 1.5).tap(A, 2.5),
        outputIs("Hello/Presses counts the first press", "Hello/Presses", 0.7, 1.4, 1, 0),
        outputIs("It counts all three presses", "Hello/Presses", 2.7, 3.5, 3, 0));

    trial(
        "first-output",
        Challenge01FirstOutput::new,
        new Script(5).tap(A, 0.5).tap(B, 3.0),
        outputIs(
            "After A, the log says Flywheel/SetpointRPM is 3000",
            "Flywheel/SetpointRPM",
            0.7,
            3.0,
            SHOT_RPM,
            0.5),
        outputIs(
            "After B, the log says Flywheel/SetpointRPM is 0",
            "Flywheel/SetpointRPM",
            3.2,
            5.0,
            0,
            0.5),
        of(
                "The value has a unit, so AdvantageScope can label its axis",
                log -> {
                  Frame frame = log.at(1.0);
                  LogValue value = frame == null ? null : frame.output("Flywheel/SetpointRPM");
                  if (value == null) {
                    return fail("nothing is logged at \"Flywheel/SetpointRPM\" yet");
                  }
                  return value.unitStr != null && !value.unitStr.isEmpty()
                      ? pass("the unit is \"" + value.unitStr + "\"")
                      : fail("add a third argument to recordOutput, like \"rpm\"");
                })
            .bonus());

    trial(
        "every-loop",
        Challenge02EveryLoop::new,
        new Script(5).hold(A, 0.5, 3.5),
        always(
            "Shooter/MeasuredRPM follows the flywheel in every loop",
            1.0,
            5.0,
            frame -> {
              Double value = frame.number("Shooter/MeasuredRPM");
              // A subsystem's periodic may run just before the flywheel's, so allow for one loop.
              return value != null && Math.abs(value - frame.world().flywheelRpm()) <= 300;
            },
            frame ->
                describeOutput(frame, "Shooter/MeasuredRPM")
                    + f(", but the flywheel was at %.0f RPM", frame.world().flywheelRpm()),
            "it tracked the flywheel the whole way"),
        all(
            "Shooter/AtSpeed is a true/false that says when the flywheel's ready",
            isType("Shooter/AtSpeed", LoggableType.Boolean, 0.3),
            becomes(
                "",
                1.0,
                3.5,
                frame -> Double.valueOf(1.0).equals(frame.number("Shooter/AtSpeed")),
                frame -> describeOutput(frame, "Shooter/AtSpeed"),
                "true")),
        all(
            "Shooter/State goes IDLE, SPINNING_UP, READY, IDLE",
            textIs("Shooter/State", "IDLE", 0.2, 0.45),
            textIs("Shooter/State", "SPINNING_UP", 0.65, 0.8),
            textIs("Shooter/State", "READY", 2.8, 3.4),
            textIs("Shooter/State", "IDLE", 3.7, 5.0)));

    trial(
        "poses",
        Challenge03Poses::new,
        new Script(7)
            .start(Challenge03Poses.WAYPOINTS[0])
            .stick(LEFT_Y, -1, 0.5, 1.5)
            .stick(LEFT_X, -0.67, 2.2, 3.2)
            .stick(RIGHT_X, -0.52, 3.8, 4.8),
        all(
            "Odometry/Robot is a Pose2d that follows the robot",
            isCustomType("Odometry/Robot", "struct:Pose2d", 0.3),
            always(
                "",
                0.3,
                7.0,
                frame -> {
                  Pose2d logged = frame.pose("Odometry/Robot");
                  return logged != null
                      && logged.getTranslation().getDistance(frame.world().pose().getTranslation())
                          < 0.2;
                },
                frame -> {
                  Pose2d logged = frame.pose("Odometry/Robot");
                  return logged == null
                      ? describeOutput(frame, "Odometry/Robot")
                      : f(
                          "the log says (%.1f, %.1f), the robot is at (%.1f, %.1f)",
                          logged.getX(),
                          logged.getY(),
                          frame.world().pose().getX(),
                          frame.world().pose().getY());
                },
                "it followed the robot")),
        of(
            "Odometry/Waypoints is an array holding all three waypoints",
            log -> {
              Frame frame = log.at(1.0);
              Pose2d[] poses = frame == null ? null : frame.poses("Odometry/Waypoints");
              if (poses == null) {
                return fail("there's no Pose2d array at \"Odometry/Waypoints\"");
              }
              if (poses.length != Challenge03Poses.WAYPOINTS.length) {
                return fail(f("it has %d poses, not 3", poses.length));
              }
              for (int i = 0; i < poses.length; i++) {
                if (!poses[i].equals(Challenge03Poses.WAYPOINTS[i])) {
                  return fail("waypoint " + i + " isn't the one in WAYPOINTS");
                }
              }
              return pass("all three are there");
            }),
        becomes(
                "Odometry/Trail is an array of where the robot has been, at least 20 poses long",
                0.3,
                7.0,
                frame -> {
                  Pose2d[] trail = frame.poses("Odometry/Trail");
                  return trail != null && trail.length >= 20;
                },
                frame -> describeOutput(frame, "Odometry/Trail"),
                "it had 20 poses")
            .bonus());

    trial(
        "auto-log-output",
        Challenge04AutoLogOutput::new,
        new Script(4.5).hold(A, 0.5, 3.0).tap(X, 1.0).tap(X, 2.0),
        all(
            "shotsRequested is annotated, and logged at its automatic key",
            annotated(Challenge04AutoLogOutput.ShooterTelemetry.class, "shotsRequested", ""),
            outputIs("", "ShooterTelemetry/ShotsRequested", 2.3, 4.5, 2, 0)),
        all(
            "targetRpm is annotated, and logged at Shooter/TargetRPM",
            annotated(
                Challenge04AutoLogOutput.ShooterTelemetry.class, "targetRpm", "Shooter/TargetRPM"),
            outputIs("", "Shooter/TargetRPM", 0.2, 4.5, SHOT_RPM, 0.5)),
        of(
            "Shooter/TargetRPM has the unit \"rpm\"",
            log -> {
              Frame frame = log.at(1.0);
              LogValue value = frame == null ? null : frame.output("Shooter/TargetRPM");
              if (value == null) {
                return fail("nothing is logged at \"Shooter/TargetRPM\"");
              }
              return "rpm".equalsIgnoreCase(value.unitStr)
                  ? pass("the unit is \"rpm\"")
                  : fail("use @AutoLogOutput(key = \"...\", unit = \"rpm\")");
            }),
        all(
            "isReady() is annotated, and logged at Shooter/Ready",
            annotatedMethod(
                Challenge04AutoLogOutput.ShooterTelemetry.class, "isReady", "Shooter/Ready"),
            becomes(
                "",
                1.0,
                3.0,
                frame -> Double.valueOf(1.0).equals(frame.number("Shooter/Ready")),
                frame -> describeOutput(frame, "Shooter/Ready"),
                "true"),
            always(
                "",
                3.3,
                4.5,
                frame -> Double.valueOf(0.0).equals(frame.number("Shooter/Ready")),
                frame -> describeOutput(frame, "Shooter/Ready"),
                "false after A is let go")));

    trialWithReplay(
        "process-inputs",
        Challenge05ProcessInputs::new,
        new Script(5).hold(A, 0.5, 3.5),
        always(
            "The input Shooter/VelocityRadPerSec follows the flywheel",
            0.3,
            5.0,
            frame -> {
              Double value = frame.inputNumber("Shooter/VelocityRadPerSec");
              return value != null
                  && Math.abs(
                          Units.radiansPerSecondToRotationsPerMinute(value)
                              - frame.world().flywheelRpm())
                      < 300;
            },
            frame -> describeInput(frame, "Shooter/VelocityRadPerSec"),
            "it tracked the flywheel"),
        becomes(
            "The shooter reaches 3000 RPM, because now it can read its own speed",
            1.0,
            3.5,
            frame -> Math.abs(frame.world().flywheelRpm() - SHOT_RPM) < 100,
            frame -> f("the flywheel was at %.0f RPM", frame.world().flywheelRpm()),
            "there"),
        replayMatches(REPLAY_MATCHES),
        noHardwareInReplay(NO_HARDWARE));

    trialWithReplay(
        "write-an-io",
        Challenge06WriteAnIO::new,
        new Script(4).piece(0.3).hold(A, 1.0, 3.0),
        all(
            "The input Intake/HasPiece follows the beam break",
            isInputType("Intake/HasPiece", LoggableType.Boolean, 0.5),
            always(
                "",
                0.5,
                1.0,
                frame -> Double.valueOf(1.0).equals(frame.inputNumber("Intake/HasPiece")),
                frame -> describeInput(frame, "Intake/HasPiece"),
                "true with a piece"),
            always(
                "",
                2.0,
                4.0,
                frame -> Double.valueOf(0.0).equals(frame.inputNumber("Intake/HasPiece")),
                frame -> describeInput(frame, "Intake/HasPiece"),
                "false without one")),
        all(
            "The inputs Intake/AppliedVolts and Intake/CurrentAmps are in the log",
            becomes(
                "",
                1.0,
                1.5,
                frame -> {
                  Double volts = frame.inputNumber("Intake/AppliedVolts");
                  return volts != null && Math.abs(volts - 6) < 0.01;
                },
                frame -> describeInput(frame, "Intake/AppliedVolts"),
                "the rollers ran"),
            becomes(
                "",
                1.0,
                1.5,
                frame -> {
                  Double amps = frame.inputNumber("Intake/CurrentAmps");
                  return amps != null && amps > 1;
                },
                frame -> describeInput(frame, "Intake/CurrentAmps"),
                "current flowed")),
        of(
            "Holding A pushes the piece out",
            log ->
                log.shots.size() == 1
                    ? pass(f("the piece left at %.2f s", log.shots.get(0).time()))
                    : fail(f("%d pieces left the feeder", log.shots.size()))),
        replayMatches(REPLAY_MATCHES),
        noHardwareInReplay(NO_HARDWARE));

    trialWithReplay(
        "no-peeking",
        Challenge07NoPeeking::new,
        new Script(5).piece(0.2).hold(A, 0.5, 4.0),
        of(
            "The robot still fires its piece once the flywheel's ready",
            log -> {
              if (log.shots.size() != 1) {
                return fail(f("%d pieces left the feeder", log.shots.size()));
              }
              return log.shots.get(0).rpm() > 2800
                  ? pass(f("fired at %.0f RPM", log.shots.get(0).rpm()))
                  : fail(f("it fired at only %.0f RPM", log.shots.get(0).rpm()));
            }),
        always(
            "The input AutoFeeder/HasPiece is in the log",
            0.2,
            5.0,
            frame -> frame.input("AutoFeeder/HasPiece") != null,
            frame -> describeInput(frame, "AutoFeeder/HasPiece"),
            "it's there"),
        replayMatches(REPLAY_MATCHES),
        noHardwareInReplay(NO_HARDWARE));

    trialWithReplay(
        "swap-io",
        Challenge08SwapIO::new,
        new Script(3.5).hold(A, 0.5, 2.5),
        of(
            "createIO gives REAL a FlywheelIOSpark",
            log -> ioFor(Constants.Mode.REAL, FlywheelIOSpark.class)),
        of(
            "createIO gives SIM a FlywheelIOSim",
            log -> ioFor(Constants.Mode.SIM, FlywheelIOSim.class)),
        of(
            "createIO gives REPLAY an IO with no hardware",
            log -> {
              FlywheelIO io = Challenge08SwapIO.createIO(Constants.Mode.REPLAY);
              return io == null || io instanceof FlywheelIOSim || io instanceof FlywheelIOSpark
                  ? fail("REPLAY should get: new FlywheelIO() {}")
                  : pass("no hardware in sight");
            }),
        replayMatches(REPLAY_MATCHES),
        noHardwareInReplay(NO_HARDWARE));

    logReplay(
        "hindsight",
        Challenge09Hindsight::new,
        PRACTICE_MATCH,
        replayMatches("Every output from the match came out the same"),
        of(
            "Analysis/ErrorRPM only exists in the replay",
            log -> {
              if (log.replay == null) {
                return fail(replayProblem(log));
              }
              boolean inReal = log.replay.real("Analysis/ErrorRPM") != null;
              boolean inReplay = log.replay.replayKeys().contains("Analysis/ErrorRPM");
              if (!inReplay) {
                return fail("the replay has nothing at \"Analysis/ErrorRPM\"");
              }
              return inReal
                  ? fail("the match's log has it too, which shouldn't be possible")
                  : pass("the robot never logged this, and now you have it");
            }),
        of(
            "It's right at every moment of the match",
            log -> {
              if (log.replay == null) {
                return fail(replayProblem(log));
              }
              return errorMatchesInputs(log.replay);
            }),
        of(
                "Analysis/ShotCount counts the match's two shots",
                log -> {
                  if (log.replay == null) {
                    return fail(replayProblem(log));
                  }
                  ReplayLog.Series series = log.replay.replay("Analysis/ShotCount");
                  if (series == null || series.values().isEmpty()) {
                    return fail("the replay has nothing at \"Analysis/ShotCount\"");
                  }
                  Object last = series.values().lastEntry().getValue();
                  return last instanceof Double count && count == 2
                      ? pass("two shots")
                      : fail("it ended at " + ReplayLog.describe(last));
                })
            .bonus());

    trialWithReplay(
        "deterministic",
        Challenge10Deterministic::new,
        new Script(4).hold(A, 0.5, 3.5),
        becomes(
            "The routine still takes two seconds to warm up",
            2.4,
            2.7,
            frame -> {
              Double elapsed = frame.number("WarmUp/ElapsedSeconds");
              return elapsed != null && elapsed >= 1.9 && elapsed <= 2.2;
            },
            frame -> describeOutput(frame, "WarmUp/ElapsedSeconds"),
            "it reached two seconds"),
        of(
            "The wobble is still there: it changes, and stays within 50 RPM",
            log -> {
              List<Double> values = new ArrayList<>();
              for (Frame frame : log.frames) {
                Double value = frame.number("WarmUp/WobbleRPM");
                if (value != null && frame.time() > 0.6 && frame.time() < 3.4) {
                  values.add(value);
                }
              }
              if (values.size() < 20) {
                return fail("\"WarmUp/WobbleRPM\" isn't being logged");
              }
              long different = values.stream().distinct().count();
              double biggest = values.stream().mapToDouble(Math::abs).max().orElse(0);
              if (different < 10) {
                return fail("it needs to keep changing: don't remove the wobble");
              }
              return biggest <= 50.001
                  ? pass(f("%d different values, up to %.0f RPM", different, biggest))
                  : fail(f("it reached %.0f RPM", biggest));
            }),
        replayMatches(REPLAY_MATCHES),
        noHardwareInReplay(NO_HARDWARE));

    trialWithReplay(
        "dashboard-inputs",
        Challenge11DashboardInputs::new,
        new Script(5)
            .hold(A, 0.5, 4.5)
            .at(2.0, () -> setTuning(4000.0))
            .at(3.5, () -> setPreset("Far")),
        Challenges::resetTuning,
        all(
            "The robot follows the dashboard: 3000 RPM, then 4000 RPM",
            outputIs("", "TunedShooter/TargetRPM", 1.0, 1.9, SHOT_RPM, 0.5),
            outputIs("", "TunedShooter/TargetRPM", 2.2, 3.4, 4000, 0.5)),
        always(
            "The dashboard's value is in the log, as an input",
            1.0,
            5.0,
            frame -> frame.input("NetworkInputs/Tuning/TargetRPM") != null,
            frame -> "nothing's logged at \"NetworkInputs/Tuning/TargetRPM\"",
            "it's there"),
        replayMatches(REPLAY_MATCHES),
        all(
                "A LoggedDashboardChooser picks the shot preset",
                becomes(
                    "",
                    0.1,
                    5.0,
                    frame -> frame.input("NetworkInputs/SmartDashboard/ShotPreset") != null,
                    frame -> "nothing's logged at \"NetworkInputs/SmartDashboard/ShotPreset\"",
                    "it's logged"),
                outputIs("", "TunedShooter/TargetRPM", 3.7, 4.5, 5000, 0.5))
            .bonus());

    trialWithReplay(
        "log-commands",
        Challenge12LogCommands::new,
        new Script(7).piece(0.1).hold(A, 0.5, 2.0).tap(X, 1.0).hold(A, 3.0, 5.0).hold(B, 4.0, 4.6),
        all(
            "CommandsAll/SpinUp is true while A is held, and false when it's let go",
            commandIs("SpinUp", true, 0.7, 2.0),
            commandIs("SpinUp", false, 2.1, 3.0)),
        all(
            "CommandsAll/Feed is true for its half second, and false when it finishes",
            commandIs("Feed", true, 1.15, 1.4),
            commandIs("Feed", false, 1.7, 7.0)),
        all(
            "Pressing B interrupts SpinUp, and the log shows it",
            commandIs("Eject", true, 4.2, 4.5),
            commandIs("SpinUp", false, 4.2, 4.5)),
        commandIs("Commands stop being true after they're interrupted", "Eject", false, 4.8, 7.0),
        replayMatches(REPLAY_MATCHES));

    trialWithReplay(
        "fast-sensors",
        Challenge13FastSensors::new,
        new Script(5)
            .at(0.0, () -> Hardware.world().setSpeeds(0, 0, 0.3))
            .bump(1.0, 80, 0.008)
            .bump(2.5, -60, 0.008)
            .bump(3.5, 70, 0.008)
            .at(4.5, () -> Hardware.world().setSpeeds(0, 0, 0)),
        always(
            "The input Gyro/YawRateRadPerSec holds all five samples from each loop",
            0.2,
            5.0,
            frame -> {
              LogValue value = frame.input("Gyro/YawRateRadPerSec");
              return value != null
                  && value.type == LoggableType.DoubleArray
                  && value.getDoubleArray().length == World.GYRO_SAMPLES_PER_LOOP;
            },
            frame -> {
              LogValue value = frame.input("Gyro/YawRateRadPerSec");
              return value == null || value.type != LoggableType.DoubleArray
                  ? "it isn't in the log as an array"
                  : f("it held %d samples", value.getDoubleArray().length);
            },
            "five samples, every loop"),
        always(
            f("Heading/YawDegrees stays within %.1f degrees of the truth", HEADING_TOLERANCE_DEG),
            0.5,
            5.0,
            frame -> {
              Double value = frame.number("Heading/YawDegrees");
              return value != null
                  && Math.abs(value - Units.radiansToDegrees(frame.world().yawRad()))
                      <= HEADING_TOLERANCE_DEG;
            },
            frame ->
                describeOutput(frame, "Heading/YawDegrees")
                    + f(
                        ", but the robot was at %.1f degrees",
                        Units.radiansToDegrees(frame.world().yawRad())),
            "it never drifted"),
        replayMatches(REPLAY_MATCHES));

    trialWithReplay(
        "from-scratch",
        Challenge14FromScratch::new,
        new Script(5.5).tap(A, 0.5).tap(B, 3.0),
        becomes(
            "The input Pivot/PositionRad is in the log",
            0.1,
            1.0,
            frame -> frame.inputNumber("Pivot/PositionRad") != null,
            frame -> describeInput(frame, "Pivot/PositionRad"),
            "found"),
        becomes(
            "The input Pivot/AppliedVolts is in the log",
            0.1,
            1.0,
            frame -> frame.inputNumber("Pivot/AppliedVolts") != null,
            frame -> describeInput(frame, "Pivot/AppliedVolts"),
            "found"),
        always(
            "A moves the pivot to 90 degrees, and it stays there",
            2.2,
            3.0,
            frame -> Math.abs(Units.radiansToDegrees(frame.world().pivotRad()) - 90) < 3,
            frame ->
                f(
                    "the pivot was at %.1f degrees",
                    Units.radiansToDegrees(frame.world().pivotRad())),
            "at 90 degrees"),
        all(
            "B moves it back to 0 degrees",
            always(
                "",
                2.6,
                3.0,
                frame -> Units.radiansToDegrees(frame.world().pivotRad()) > 60,
                frame -> "it never got up there in the first place",
                ""),
            always(
                "",
                4.8,
                5.5,
                frame -> Units.radiansToDegrees(frame.world().pivotRad()) < 3,
                frame ->
                    f(
                        "the pivot was at %.1f degrees",
                        Units.radiansToDegrees(frame.world().pivotRad())),
                "at 0 degrees")),
        all(
            "The output Pivot/SetpointDegrees says where it was told to go",
            outputIs("", "Pivot/SetpointDegrees", 0.8, 3.0, 90, 0.5),
            outputIs("", "Pivot/SetpointDegrees", 3.3, 5.5, 0, 0.5)),
        replayMatches(REPLAY_MATCHES),
        noHardwareInReplay(NO_HARDWARE),
        becomes(
                "A method marked @AutoLogOutput logs Pivot/AtSetpoint",
                1.5,
                3.0,
                frame -> Double.valueOf(1.0).equals(frame.number("Pivot/AtSetpoint")),
                frame -> describeOutput(frame, "Pivot/AtSetpoint"),
                "true")
            .bonus());

    logReplay(
        "mystery",
        Challenge15Mystery::new,
        QUAL_17,
        of(
            "In your replay, the robot never starts feeding a piece before the flywheel's ready",
            log -> {
              if (log.replay == null) {
                return fail(replayProblem(log));
              }
              return feedsOnlyWhenReady(log.replay);
            }),
        of(
            "All three shots still happen",
            log -> {
              if (log.replay == null) {
                return fail(replayProblem(log));
              }
              int starts = feedStarts(log.replay).size();
              return starts == 3 ? pass("three shots") : fail(f("the robot fed %d times", starts));
            }),
        of(
                "You logged how far the flywheel was from its target, at Shooter/ErrorRPM",
                log -> {
                  if (log.replay == null) {
                    return fail(replayProblem(log));
                  }
                  return log.replay.replayKeys().contains("Shooter/ErrorRPM")
                      ? pass("that's the evidence")
                      : fail("the replay has nothing at \"Shooter/ErrorRPM\"");
                })
            .bonus());

    // These two aren't on the dashboard. They're how the shipped logs were made: see the source
    // folder's README.
    recording(
        "record-practice-match",
        Challenge09Hindsight::new,
        new Script(8).piece(0.2).hold(A, 0.5, 7.5).hold(X, 2.5, 3.2).piece(3.5).hold(X, 5.0, 5.7));
    recording(
        "record-qual-17",
        Challenge15Mystery::new,
        new Script(14)
            .piece(0.2)
            .tap(X, 0.5)
            .tap(A, 3.0)
            .piece(4.0)
            .tap(Y, 5.0)
            .tap(A, 5.06)
            .piece(8.0)
            .tap(A, 10.0));
  }

  private Challenges() {}

  private static void trial(
      String id, Supplier<LabChallenge> factory, Script script, Check... checks) {
    SPECS.put(id, new Spec(id, factory, script, List.of(checks), false, null, null));
  }

  private static void trialWithReplay(
      String id, Supplier<LabChallenge> factory, Script script, Check... checks) {
    SPECS.put(id, new Spec(id, factory, script, List.of(checks), true, null, null));
  }

  private static void trialWithReplay(
      String id, Supplier<LabChallenge> factory, Script script, Runnable prepare, Check... checks) {
    SPECS.put(id, new Spec(id, factory, script, List.of(checks), true, null, prepare));
  }

  private static void logReplay(
      String id, Supplier<LabChallenge> factory, String log, Check... checks) {
    SPECS.put(id, new Spec(id, factory, new Script(0), List.of(checks), true, log, null));
  }

  private static void recording(String id, Supplier<LabChallenge> factory, Script script) {
    SPECS.put(
        id,
        new Spec(
            id,
            factory,
            script,
            List.of(replayMatches("The recording replays")),
            true,
            null,
            null));
  }

  /** Every challenge id, in order, without the ones that only make the shipped logs. */
  static List<String> ids() {
    List<String> ids = new ArrayList<>();
    for (String id : SPECS.keySet()) {
      if (!id.startsWith("record-")) {
        ids.add(id);
      }
    }
    return ids;
  }

  static Spec get(String id) {
    return SPECS.get(id);
  }

  static void prepare(Spec spec) {
    if (spec.prepare() != null) {
      spec.prepare().run();
    }
  }

  private static String replayProblem(TrialLog log) {
    return log.replayError.isEmpty() ? "the replay didn't run" : log.replayError;
  }

  // Dashboard inputs ----------------------------------------------------------------------------

  private static void setTuning(double rpm) {
    NetworkTableInstance nt = NetworkTableInstance.getDefault();
    nt.getEntry("/Tuning/TargetRPM").setDouble(rpm);
    nt.getEntry("/SmartDashboard/TargetRPM").setDouble(rpm);
  }

  private static void setPreset(String name) {
    NetworkTableInstance.getDefault()
        .getEntry("/SmartDashboard/ShotPreset/selected")
        .setString(name);
  }

  private static void resetTuning() {
    setTuning(SHOT_RPM);
    setPreset("Near");
  }

  // Check helpers -------------------------------------------------------------------------------

  private static Check textIs(String key, String expected, double from, double to) {
    return always(
        "",
        from,
        to,
        frame -> expected.equals(frame.text(key)),
        frame -> {
          String value = frame.text(key);
          return value == null
              ? describeOutput(frame, key)
              : f("\"%s\" was %s, not %s", key, value, expected);
        },
        key + " was " + expected);
  }

  private static Check isType(String key, LoggableType type, double at) {
    return of(
        "",
        log -> {
          Frame frame = log.at(at);
          LogValue value = frame == null ? null : frame.output(key);
          if (value == null) {
            return fail(f("nothing is logged at \"%s\"", key));
          }
          return value.type == type
              ? pass("")
              : fail(f("\"%s\" is logged as %s", key, LogAccess.typeName(value)));
        });
  }

  private static Check isInputType(String key, LoggableType type, double at) {
    return of(
        "",
        log -> {
          Frame frame = log.at(at);
          LogValue value = frame == null ? null : frame.input(key);
          if (value == null) {
            return fail(f("there's no input at \"%s\": is processInputs called every loop?", key));
          }
          return value.type == type
              ? pass("")
              : fail(f("the input \"%s\" is logged as %s", key, LogAccess.typeName(value)));
        });
  }

  private static Check isCustomType(String key, String type, double at) {
    return of(
        "",
        log -> {
          Frame frame = log.at(at);
          LogValue value = frame == null ? null : frame.output(key);
          if (value == null) {
            return fail(f("nothing is logged at \"%s\"", key));
          }
          return type.equals(value.customTypeStr)
              ? pass("")
              : fail(f("\"%s\" is logged as %s", key, LogAccess.typeName(value)));
        });
  }

  private static Check annotated(Class<?> type, String fieldName, String key) {
    return of(
        "",
        log -> {
          try {
            Field field = type.getDeclaredField(fieldName);
            return checkAnnotation(field.getAnnotation(AutoLogOutput.class), fieldName, key);
          } catch (NoSuchFieldException e) {
            return fail("the field \"" + fieldName + "\" is gone");
          }
        });
  }

  private static Check annotatedMethod(Class<?> type, String methodName, String key) {
    return of(
        "",
        log -> {
          try {
            Method method = type.getDeclaredMethod(methodName);
            return checkAnnotation(method.getAnnotation(AutoLogOutput.class), methodName, key);
          } catch (NoSuchMethodException e) {
            return fail("the method \"" + methodName + "\" is gone");
          }
        });
  }

  private static Check.Outcome checkAnnotation(AutoLogOutput annotation, String name, String key) {
    if (annotation == null) {
      return fail("\"" + name + "\" doesn't have an @AutoLogOutput annotation");
    }
    return key.isEmpty() || key.equals(annotation.key())
        ? pass("")
        : fail(f("\"%s\" should use key = \"%s\"", name, key));
  }

  private static Check.Outcome ioFor(Constants.Mode mode, Class<?> expected) {
    FlywheelIO io = Challenge08SwapIO.createIO(mode);
    return expected.isInstance(io)
        ? pass("right")
        : fail(
            f(
                "got %s",
                io == null || io.getClass().isAnonymousClass()
                    ? "something else"
                    : io.getClass().getSimpleName()));
  }

  private static Check commandIs(String name, boolean active, double from, double to) {
    return commandIs("", name, active, from, to);
  }

  private static Check commandIs(
      String label, String name, boolean active, double from, double to) {
    String key = "CommandsAll/" + name;
    Predicate<Frame> test =
        frame -> {
          Double value = frame.number(key);
          return value != null && (value == 1.0) == active;
        };
    return always(
        label,
        from,
        to,
        test,
        frame ->
            frame.output(key) == null
                ? f("nothing is logged at \"%s\"", key)
                : f("\"%s\" was %s", key, !active),
        f("%s was %s", key, active));
  }

  private static Check.Outcome errorMatchesInputs(ReplayLog replay) {
    ReplayLog.Series error = replay.replay("Analysis/ErrorRPM");
    ReplayLog.Series velocity = replay.input("/Flywheel/VelocityRadPerSec");
    if (error == null || error.values().isEmpty()) {
      return fail("the replay has nothing at \"Analysis/ErrorRPM\"");
    }
    if (velocity == null) {
      return fail("the log has no flywheel speed to compare it with");
    }
    // While the driver holds A, from 0.5 s to 7.5 s, the target is 3000 RPM. Stay clear of the very
    // moment it changes.
    long start = replay.startTime();
    for (double t = 1.0; t < 7.0; t += 0.02) {
      long time = start + (long) (t * 1e6);
      Object logged = error.at(time);
      Object measured = velocity.at(time);
      if (!(logged instanceof Double got) || !(measured instanceof Double radPerSec)) {
        return fail(f("at %.2f s it isn't a number", t));
      }
      double expected = SHOT_RPM - Units.radiansPerSecondToRotationsPerMinute(radPerSec);
      if (Math.abs(got - expected) > 1) {
        return fail(f("at %.2f s it was %.1f, and target minus speed was %.1f", t, got, expected));
      }
    }
    return pass("it's the target minus the speed, all match long");
  }

  /** The moments the replayed code starts feeding a piece. */
  private static List<Long> feedStarts(ReplayLog replay) {
    List<Long> starts = new ArrayList<>();
    ReplayLog.Series feeding = replay.replay("Shooter/Feeding");
    if (feeding == null) {
      return starts;
    }
    boolean was = false;
    for (Map.Entry<Long, Object> entry : feeding.values().entrySet()) {
      boolean is = Boolean.TRUE.equals(entry.getValue());
      if (is && !was) {
        starts.add(entry.getKey());
      }
      was = is;
    }
    return starts;
  }

  private static Check.Outcome feedsOnlyWhenReady(ReplayLog replay) {
    ReplayLog.Series target = replay.replay("Shooter/TargetRPM");
    ReplayLog.Series velocity = replay.input("/Shooter/FlywheelRadPerSec");
    if (target == null || velocity == null) {
      return fail("the replay has no \"Shooter/TargetRPM\" or flywheel speed");
    }
    List<Long> starts = feedStarts(replay);
    if (starts.isEmpty()) {
      return fail("the robot never fed in your replay");
    }
    for (long start : starts) {
      Object wanted = target.at(start);
      Object speed = velocity.at(start);
      if (wanted instanceof Double rpm && speed instanceof Double radPerSec) {
        double error = rpm - Units.radiansPerSecondToRotationsPerMinute(radPerSec);
        if (Math.abs(error) > 200) {
          return fail(
              f(
                  "at %.2f s it started feeding %.0f RPM away from the target",
                  replay.seconds(start), Math.abs(error)));
        }
      }
    }
    return pass(f("%d feeds, all at the right speed", starts.size()));
  }
}
