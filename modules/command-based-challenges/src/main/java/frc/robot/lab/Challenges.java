package frc.robot.lab;

import static frc.robot.lab.Check.all;
import static frc.robot.lab.Check.always;
import static frc.robot.lab.Check.becomes;
import static frc.robot.lab.Check.f;
import static frc.robot.lab.Check.fail;
import static frc.robot.lab.Check.of;
import static frc.robot.lab.Check.pass;
import static frc.robot.lab.Script.A;
import static frc.robot.lab.Script.B;
import static frc.robot.lab.Script.LB;
import static frc.robot.lab.Script.LEFT_TRIGGER;
import static frc.robot.lab.Script.LEFT_X;
import static frc.robot.lab.Script.LEFT_Y;
import static frc.robot.lab.Script.RB;
import static frc.robot.lab.Script.RIGHT_TRIGGER;
import static frc.robot.lab.Script.RIGHT_X;
import static frc.robot.lab.Script.X;
import static frc.robot.lab.Script.Y;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import frc.robot.FieldConstants;
import frc.robot.challenges.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Every challenge the dashboard can run: its class, the trial's script (where the robot starts, and
 * what the scripted driver presses when), and what the trial is graded on.
 *
 * <p>The ids must match the dashboard's list of challenges. You don't need to edit this file.
 */
public final class Challenges {
  /**
   * @param freeDrive in free play, give the robot simple stick driving if the challenge doesn't set
   *     a default command for the drive
   */
  record Spec(
      String id,
      Supplier<CommandChallenge> factory,
      Script script,
      List<Check> checks,
      boolean freeDrive) {}

  private static final Map<String, Spec> SPECS = new LinkedHashMap<>();

  private static final double SHOT_RPM = 3000;

  static {
    add(
        "example",
        Challenge00Example::new,
        new Script(3.5).hold(X, 0.5, 2.0).tap(Y, 2.5),
        true,
        always(
            "Holding X spins the intake rollers",
            0.6,
            2.0,
            f -> f.rollers() >= 0.5,
            f -> f("the rollers were at %.1f", f.rollers()),
            "the rollers spun the whole time X was held"),
        always(
            "Letting go of X stops them",
            2.1,
            3.5,
            f -> f.rollers() == 0,
            f -> f("the rollers were still at %.1f", f.rollers()),
            "the rollers stopped"));

    add(
        "spin-up",
        Challenge01SpinUp::new,
        new Script(5).tap(A, 0.5).tap(B, 3.0),
        true,
        startsCommandRequiring(
            "A starts a command that requires the shooter",
            "Pressing A",
            0.5,
            0.62,
            "Shooter",
            "Make it with shooter.runOnce(...) so the scheduler knows it uses the shooter."),
        always(
            "The shooter spins up to 3000 RPM, and keeps spinning after A is let go",
            1.8,
            3.0,
            f -> near(f.targetRpm(), SHOT_RPM) && f.atSpeed(),
            Challenges::shooterState,
            "at speed"),
        always(
            "B stops the shooter",
            3.15,
            5.0,
            f -> f.targetRpm() == 0,
            f -> f("the shooter was still asked for %.0f RPM", f.targetRpm()),
            "stopped"),
        namedCommands("Your commands have names (use .withName(...))", 0, 5, "Shooter").bonus());

    add(
        "hold-to-intake",
        Challenge02HoldToIntake::new,
        new Script(5).hold(LB, 0.5, 2.5).hold(LB, 3.5, 4.0),
        true,
        startsCommandRequiring(
            "Holding LB starts a command that requires the intake",
            "Holding LB",
            0.5,
            0.62,
            "Intake",
            "Make it with intake.startEnd(...) or intake.runEnd(...)."),
        all(
            "While LB is held, the intake is out and its rollers pull in",
            always(0.85, 2.5, Frame::intaking, Challenges::intakeState, "intaking"),
            always(3.85, 4.0, Frame::intaking, Challenges::intakeState, "intaking")),
        all(
            "Letting go of LB stops the rollers and pulls the intake back in",
            always(2.6, 3.5, Challenges::intakeIdle, Challenges::intakeState, "stopped"),
            always(4.1, 5.0, Challenges::intakeIdle, Challenges::intakeState, "stopped")),
        namedCommands("Your command has a name (use .withName(...))", 0, 5, "Intake").bonus());

    add(
        "toggle",
        Challenge03Toggle::new,
        new Script(6).tap(Y, 0.5).tap(Y, 2.5).tap(Y, 4.0).tap(X, 5.2),
        true,
        always(
            "The first press of Y spins the shooter up to 3000 RPM",
            1.6,
            2.5,
            f -> near(f.targetRpm(), SHOT_RPM) && f.atSpeed(),
            Challenges::shooterState,
            "at speed"),
        always(
            "The second press of Y stops it",
            2.65,
            4.0,
            f -> f.targetRpm() == 0,
            Challenges::shooterState,
            "stopped"),
        always(
            "The third press spins it up again",
            4.15,
            5.2,
            f -> near(f.targetRpm(), SHOT_RPM),
            Challenges::shooterState,
            "spinning"),
        always(
                "X stops the shooter, even after Y turned it on",
                5.35,
                6.0,
                f -> f.targetRpm() == 0,
                Challenges::shooterState,
                "stopped")
            .bonus());

    add(
        "drive-with-sticks",
        Challenge04DriveWithSticks::new,
        new Script(7)
            .start(new Pose2d(2.0, 1.5, Rotation2d.kZero))
            .stick(LEFT_Y, -1, 0.5, 1.3)
            .stick(LEFT_X, -0.5, 1.8, 2.4)
            .stick(RIGHT_X, 1, 2.9, 3.15)
            .stick(LEFT_Y, -0.5, 4.6, 5.2)
            .stick(LEFT_X, 0.06, 6.0, 7.0),
        false,
        moves(
            "Pushing the left stick up drives forward, away from the driver station (+X)",
            0.5,
            1.8,
            1.5,
            0),
        moves("Pushing the left stick left drives left (+Y)", 1.8, 2.9, 0, 0.6),
        of(
            "Pushing the right stick right turns the robot clockwise",
            log -> {
              double turned = turned(log, 2.9, 3.6);
              return turned <= -30
                  ? pass(f("turned %.0f°", turned))
                  : fail(f("the robot turned %.0f° (clockwise is negative)", turned));
            }),
        always(
            "Letting go of the sticks stops the robot",
            4.2,
            4.6,
            f -> f.speed() < 0.05 && Math.abs(f.omega()) < 0.05,
            f -> f("still moving at %.2f m/s, turning at %.2f rad/s", f.speed(), f.omega()),
            "stopped"),
        moves("Field-relative: after turning, pushing up still drives +X", 4.6, 5.8, 0.5, 0)
            .bonus(),
        always(
                "A slightly drifting stick doesn't creep the robot along (use a deadband)",
                6.3,
                7.0,
                f -> f.speed() < 0.02,
                f -> f("the robot crept along at %.2f m/s", f.speed()),
                "stayed still")
            .bonus());

    add(
        "intake-until-loaded",
        Challenge05IntakeUntilLoaded::new,
        new Script(6).hold(LB, 0.5, 4.0).rollIn(1.2).hold(LB, 4.6, 5.4),
        true,
        always(
            "While waiting for a piece, the intake is out and the rollers and indexer run",
            0.85,
            1.2,
            f -> f.intaking() && f.indexer() >= 0.3,
            f -> intakeState(f) + f(", indexer at %.1f", f.indexer()),
            "ready"),
        becomes(
            "Pulls the piece in to the beam break",
            1.2,
            2.5,
            Frame::beamBreak,
            f ->
                f.hasPiece()
                    ? "the piece is stuck inside the robot. Is the indexer running?"
                    : "there's no piece in the robot",
            "loaded"),
        of(
            "Stops with the piece at the beam break (doesn't push it into the shooter)",
            log -> {
              if (!log.shots.isEmpty()) {
                return fail(
                    f(
                        "at %.2f s the indexer pushed the piece into the shooter",
                        log.shots.get(0).launched()));
              }
              Frame end = log.at(4.0);
              return end.beamBreak() ? pass("held at the beam break") : fail("no piece at 4 s");
            }),
        always(
            "Once loaded, the intake retracts and everything stops, even with LB still held",
            2.0,
            4.0,
            Challenges::intakeAndIndexerIdle,
            f -> intakeState(f) + f(", indexer at %.1f", f.indexer()),
            "stopped"),
        always(
                "Pressing LB with a piece already loaded does nothing",
                4.6,
                5.6,
                Challenges::intakeAndIndexerIdle,
                f -> intakeState(f) + f(", indexer at %.1f", f.indexer()),
                "ignored")
            .bonus());

    add(
        "shoot",
        Challenge06Shoot::new,
        new Script(4).preload().squeeze(RIGHT_TRIGGER, 0.5),
        true,
        scores("Scores the piece", 0, 4, 1),
        always(
            "Waits until the shooter is at speed before feeding",
            0,
            4,
            f -> !(f.indexer() > 0.1 && f.hasPiece() && !f.atSpeed()),
            f -> f("the indexer was feeding with the shooter at %.0f RPM", f.rpm()),
            "waited"),
        always(
            "Stops the shooter and the indexer after the shot",
            3.3,
            4.0,
            f -> f.targetRpm() == 0 && f.indexer() == 0,
            f -> shooterState(f) + f(", indexer at %.1f", f.indexer()),
            "stopped"),
        launchedBy("Shoots within 1.2 s of pulling the trigger", 1.7).bonus());

    add(
        "ready-set-shoot",
        Challenge07ReadySetShoot::new,
        new Script(6).hold(RB, 0.5, 4.0).rollIn(1.6).hold(RB, 4.5, 5.0),
        true,
        always(
            "Spins up the shooter while the intake waits for a piece",
            1.5,
            1.6,
            f -> f.atSpeed() && f.intaking(),
            f -> shooterState(f) + ", " + intakeState(f),
            "both"),
        of(
            "Shoots as soon as the piece is loaded",
            log -> {
              double loaded = log.first(1.6, 4.0, Frame::beamBreak);
              if (Double.isNaN(loaded)) {
                return fail("the piece never reached the beam break");
              }
              List<World.Shot> shots = log.shots(1.6, 4.0);
              if (shots.isEmpty()) {
                return fail(f("loaded at %.2f s, but never shot", loaded));
              }
              double wait = shots.get(0).launched() - loaded;
              return wait <= 0.35
                  ? pass(f("shot %.2f s after loading", wait))
                  : fail(f("shot %.2f s after loading. Was the shooter already spinning?", wait));
            }),
        scores("Scores", 1.6, 4.0, 1),
        all(
            "Letting go of RB stops everything",
            always(4.1, 4.5, Challenges::allIdle, Challenges::everything, "stopped"),
            always(5.1, 6.0, Challenges::allIdle, Challenges::everything, "stopped")),
        launchedBy("Shoots within 0.6 s of the piece rolling in", 2.2).bonus());

    add(
        "whos-in-charge",
        Challenge08WhosInCharge::new,
        new Script(6.5)
            .preload()
            .squeeze(RIGHT_TRIGGER, 0.5)
            .hold(LB, 0.9, 1.3)
            .hold(LB, 2.5, 3.6)
            .rollIn(2.8)
            .squeeze(RIGHT_TRIGGER, 4.0)
            .tap(B, 4.4),
        true,
        scores("Pressing LB in the middle of a shot doesn't stop it: the piece scores", 0, 2.5, 1),
        becomes(
            "Once the shot is done, LB intakes again",
            2.8,
            3.6,
            Frame::beamBreak,
            f -> f.hasPiece() ? "the piece didn't reach the beam break" : "no piece was picked up",
            "loaded"),
        always(
            "B cancels a shot that's spinning up: the shooter and indexer stop",
            4.55,
            6.5,
            f -> f.targetRpm() == 0 && f.indexer() == 0 && f.beamBreak(),
            f ->
                !f.beamBreak()
                    ? "the piece was shot anyway"
                    : shooterState(f) + f(", indexer at %.1f", f.indexer()),
            "cancelled"));

    add(
        "led-signals",
        Challenge09LedSignals::new,
        new Script(7).hold(LB, 0.5, 2.0).rollIn(0.8).tap(A, 2.5).tap(B, 4.5),
        true,
        always(
            "With no piece, the LEDs show your alliance color",
            0.1,
            0.8,
            Challenges::allianceColor,
            f -> f("the LEDs were %s%s", f.ledName(), allianceNote(f)),
            "alliance color"),
        ledColor("Green while holding a piece", 1.4, 3.3, "green"),
        ledColor("White while the shooter is at speed", 3.6, 4.5, "white"),
        ledColor(
            "Back to green when the shooter stops (you still have the piece)", 4.7, 7, "green"),
        of(
                "Rumbles the controller when a piece loads, then stops",
                log -> {
                  double start = log.first(0.8, 1.8, f -> f.rumble() > 0);
                  if (Double.isNaN(start)) {
                    return fail("no rumble after the piece loaded");
                  }
                  Frame still = log.firstFailure(2.4, 2.5, f -> f.rumble() == 0);
                  return still == null
                      ? pass(f("rumbled at %.2f s", start))
                      : fail("still rumbling at 2.4 s");
                })
            .bonus());

    add(
        "smart-triggers",
        Challenge10SmartTriggers::new,
        new Script(8)
            .squeeze(RIGHT_TRIGGER, 0.5)
            .hold(LB, 1.0, 2.5)
            .rollIn(1.2)
            .squeeze(RIGHT_TRIGGER, 3.0)
            .jam(4.9)
            .hold(LB, 5.0, 6.0)
            .rollIn(5.2)
            .hold(LB, 6.5, 7.5),
        true,
        always(
            "RT does nothing when nothing is loaded",
            0.5,
            1.0,
            f -> f.targetRpm() == 0 && f.indexer() == 0,
            f -> shooterState(f) + f(", indexer at %.1f", f.indexer()),
            "nothing"),
        scores("RT shoots when a piece is loaded", 3.0, 5.0, 1),
        always(
            "A normal pickup doesn't set off the anti-jam (debounce it)",
            1.0,
            3.0,
            f -> f.rollers() >= 0,
            f -> "the rollers ran backwards",
            "no false alarm"),
        of(
            "A jammed piece gets cleared by running the rollers backwards",
            log -> {
              double jam = log.first(5.2, 5.7, Frame::jammed);
              if (Double.isNaN(jam)) {
                return fail("the piece never jammed. Was the intake running?");
              }
              Frame after = log.at(6.4);
              if (after.jammed()) {
                return fail("the piece was still jammed at 6.4 s");
              }
              double reversed = log.first(jam, 6.4, f -> f.rollers() < 0);
              return pass(f("jammed at %.2f s, reversed at %.2f s", jam, reversed));
            }),
        becomes(
            "Once the jam is cleared, LB loads the piece",
            6.5,
            7.6,
            Frame::beamBreak,
            f -> f.hasPiece() ? "the piece didn't reach the beam break" : "the piece was lost",
            "loaded"),
        of(
                "RT with nothing loaded flashes the LEDs red, then turns them off",
                log -> {
                  double red = log.first(0.5, 1.0, f -> f.ledName().equals("red"));
                  if (Double.isNaN(red)) {
                    return fail("the LEDs didn't turn red");
                  }
                  Frame stuck = log.firstFailure(1.4, 2.0, f -> !f.ledName().equals("red"));
                  return stuck == null
                      ? pass(f("red at %.2f s", red))
                      : fail(f("still red at %.2f s", stuck.time()));
                })
            .bonus());

    add(
        "climber",
        Challenge11Climber::new,
        new Script(8)
            .start(new Pose2d(FieldConstants.TOWER, Rotation2d.kZero))
            .pov(0, 0.5, 0.6)
            .pov(180, 1.0, 1.1)
            .pov(0, 2.0, 2.1)
            .pov(0, 4.0, 4.5)
            .pov(180, 5.0, 5.1),
        true,
        startsCommandRequiring(
            "Up on the d-pad starts a command that requires the climber",
            "Pressing up on the d-pad",
            0.5,
            0.62,
            "Climber",
            "Make the climber's commands in Climber.java with run(...), runEnd(...) and so on."),
        always(
            "Extends to the top, then stops",
            3.5,
            4.0,
            f -> f.climberHeight() >= 0.5 && Math.abs(f.climberVolts()) < 0.5,
            Challenges::climberState,
            "at the top"),
        of(
            "Pressing down on the way up reverses it right away",
            log -> {
              double reversed = log.first(1.0, 1.15, f -> f.climberVolts() < -1);
              if (Double.isNaN(reversed)) {
                return fail("pressing down at 1.0 s didn't make it go down");
              }
              Frame down = log.at(2.0);
              return down.climberHeight() <= 0.01
                  ? pass(f("reversed at %.2f s", reversed))
                  : fail("by 2.0 s: " + climberState(down));
            }),
        always(
            "Never pushes against the hard stops for more than 0.2 s",
            0,
            8,
            f -> f.strain() <= 0.2,
            f -> "the motor was pushing against the end of its travel",
            "no strain"),
        becomes(
            "Climbs: lifts the robot all the way up, then stops",
            5.0,
            8.0,
            f -> f.hang() >= 0.99 && Math.abs(f.climberVolts()) < 0.5,
            Challenges::climberState,
            "hanging"),
        namedCommands("Your climber's commands have names", 0, 8, "Climber").bonus());

    add(
        "aim-at-hub",
        Challenge12AimAtHub::new,
        new Script(5)
            .start(new Pose2d(2.5, 2.0, Rotation2d.k180deg))
            .stick(LEFT_TRIGGER, 1, 0.5, 3.5)
            .stick(LEFT_X, -0.5, 1.8, 2.6)
            .stick(RIGHT_X, 0.6, 4.0, 4.5),
        false,
        becomes(
            "Turns to face the hub, and settles there, within 1 s of pulling LT",
            0.5,
            1.5,
            f -> Math.abs(aimError(f)) < 3 && Math.abs(f.omega()) < 1,
            f -> f("%.0f° away from facing the hub, turning at %.1f rad/s", aimError(f), f.omega()),
            "on target"),
        of(
            "Keeps facing the hub while the driver drives sideways",
            log -> {
              Frame off = log.firstFailure(1.9, 3.5, f -> Math.abs(aimError(f)) < 5);
              if (off != null) {
                return fail(f("at %.2f s: %.0f° off the hub", off.time(), aimError(off)));
              }
              double moved = log.at(3.0).pose().getY() - log.at(1.8).pose().getY();
              return moved >= 0.8
                  ? pass(f("moved %.1f m sideways, on target", moved))
                  : fail(f("on target, but the robot only moved %.2f m sideways", moved));
            }),
        of(
            "Normal driving comes back when LT is let go",
            log -> {
              double turned = turned(log, 4.0, 4.8);
              return turned <= -20
                  ? pass(f("the right stick turned the robot %.0f°", turned))
                  : fail(f("the right stick only turned the robot %.0f°", turned));
            }),
        becomes(
                "Settles on the hub within 0.8 s, within 2°",
                0.5,
                1.3,
                f -> Math.abs(aimError(f)) < 2 && Math.abs(f.omega()) < 0.3,
                f -> f("%.0f° off, turning at %.1f rad/s", aimError(f), f.omega()),
                "settled")
            .bonus());

    add(
        "timed-auto",
        Challenge13TimedAuto::new,
        new Script(15).autonomous().preload().piece(3.3, FieldConstants.HUB.getY()),
        true,
        scores("Scores the preloaded piece", 0, 15, 1),
        of(
            "Drives at least 1 m away from where it started",
            log -> {
              Pose2d start = log.frames.get(0).pose();
              double farthest = 0;
              for (Frame frame : log.frames) {
                farthest =
                    Math.max(
                        farthest,
                        frame.pose().getTranslation().getDistance(start.getTranslation()));
              }
              return farthest >= 1.0
                  ? pass(f("went %.1f m", farthest))
                  : fail(f("the robot only got %.2f m from its start", farthest));
            }),
        always(
            "Stopped by the end of autonomous",
            14.0,
            15.0,
            f -> f.speed() < 0.05 && Math.abs(f.omega()) < 0.05,
            f -> f("still moving at %.2f m/s", f.speed()),
            "stopped"),
        scores("Scores a second piece (the one between you and the hub)", 0, 15, 2).bonus());

    add(
        "pathplanner-auto",
        Challenge14PathPlannerAuto::new,
        new Script(15).autonomous().preload().piece(2.9, 5.9).piece(2.9, 2.17),
        true,
        scores("Scores the preloaded piece", 0, 15, 1),
        of(
            "Picks up a piece from the field",
            log -> {
              int pickups = log.last().pickups();
              return pickups >= 1 ? pass(f("picked up %d", pickups)) : fail("no pieces picked up");
            }),
        scores("Scores 2 pieces", 0, 15, 2),
        scores("Scores 3 pieces", 0, 15, 3).bonus());

    add(
        "match-play",
        Challenge15MatchPlay::new,
        Script.freePlay().preload().driverControlled().withDuration(60),
        false,
        scores("Scores 3 pieces in a minute", 0, 60, 3),
        scores("Scores 6 pieces", 0, 60, 6).bonus(),
        always(
                "Hangs from the tower when time runs out",
                59.9,
                60,
                f -> f.hang() >= 0.99,
                f -> "not hanging",
                "hanging")
            .bonus());
  }

  private Challenges() {}

  private static void add(
      String id,
      Supplier<CommandChallenge> factory,
      Script script,
      boolean freeDrive,
      Check... checks) {
    SPECS.put(id, new Spec(id, factory, script, List.of(checks), freeDrive));
  }

  /** Every challenge id, in order. */
  static List<String> ids() {
    return new ArrayList<>(SPECS.keySet());
  }

  /** The challenge with this id, or null. */
  static Spec get(String id) {
    return SPECS.get(id);
  }

  // Checks used by several challenges ---------------------------------------------------------

  private static Check startsCommandRequiring(
      String label, String what, double from, double to, String subsystem, String hint) {
    return of(
        label,
        log -> {
          List<CommandTracker.Event> started = log.events(CommandTracker.Type.START, from, to);
          if (started.isEmpty()) {
            return fail(what + " didn't start any command. Is it bound in configureBindings()?");
          }
          for (CommandTracker.Event event : started) {
            if (event.requirements().contains(subsystem)) {
              return pass(f("\"%s\" requires %s", event.name(), describe(event.requirements())));
            }
          }
          CommandTracker.Event first = started.get(0);
          return fail(
              f(
                  "%s started \"%s\", which requires %s. %s",
                  what, first.name(), describe(first.requirements()), hint));
        });
  }

  /** Every command that used one of these subsystems was given a name. */
  private static Check namedCommands(String label, double from, double to, String... subsystems) {
    return of(
        label,
        log -> {
          int count = 0;
          for (CommandTracker.Event event : log.events(CommandTracker.Type.START, from, to)) {
            boolean uses = false;
            for (String subsystem : subsystems) {
              uses |= event.requirements().contains(subsystem);
            }
            if (!uses) {
              continue;
            }
            count++;
            if (CommandTracker.isDefaultName(event.name())) {
              return fail(f("a command is still called \"%s\"", event.name()));
            }
          }
          return count == 0 ? fail("no commands ran") : pass("all named");
        });
  }

  /** At least {@code count} pieces launched in [from, to) went in. */
  private static Check scores(String label, double from, double to, int count) {
    return of(
        label,
        log -> {
          List<World.Shot> shots = log.shots(from, to);
          int scored = 0;
          for (World.Shot shot : shots) {
            if (shot.outcome() == World.Shot.SCORED && shot.launched() + shot.flight() <= to + 1) {
              scored++;
            }
          }
          if (scored >= count) {
            return pass(count == 1 ? "scored" : f("scored %d", scored));
          }
          if (shots.isEmpty()) {
            return fail(scored == 0 ? "nothing was shot" : f("scored %d", scored));
          }
          World.Shot last = shots.get(shots.size() - 1);
          String why =
              switch (last.outcome()) {
                case World.Shot.DRIBBLED -> f(
                    "it dribbled out: the shooter was only at %.0f RPM when the piece got there",
                    last.rpm());
                case World.Shot.MISSED -> f("the last shot missed (fired at %.0f RPM)", last.rpm());
                default -> "";
              };
          return fail(f("scored %d of %d", scored, count) + (why.isEmpty() ? "" : ": " + why));
        });
  }

  private static Check launchedBy(String label, double time) {
    return of(
        label,
        log ->
            log.shots.isEmpty()
                ? fail("nothing was shot")
                : log.shots.get(0).launched() <= time + 1e-6
                    ? pass(f("shot at %.2f s", log.shots.get(0).launched()))
                    : fail(f("shot at %.2f s", log.shots.get(0).launched())));
  }

  private static Check moves(String label, double from, double to, double minX, double minY) {
    return of(
        label,
        log -> {
          Pose2d a = log.at(from).pose();
          Pose2d b = log.at(to).pose();
          double dx = b.getX() - a.getX();
          double dy = b.getY() - a.getY();
          boolean ok =
              minX > 0 ? dx >= minX && Math.abs(dy) < 0.3 : dy >= minY && Math.abs(dx) < 0.3;
          String detail = f("moved %+.2f m in X and %+.2f m in Y", dx, dy);
          return ok ? pass(detail) : fail("the robot " + detail);
        });
  }

  private static Check ledColor(String label, double from, double to, String color) {
    return always(
        label,
        from,
        to,
        f -> f.ledName().equals(color),
        f -> f("the LEDs were %s", f.ledName()),
        color);
  }

  /** How far the robot turned between two times, in degrees (counterclockwise is positive). */
  private static double turned(TrialLog log, double from, double to) {
    double total = 0;
    Frame previous = log.at(from);
    for (Frame frame : log.between(from, to)) {
      total +=
          MathUtil.angleModulus(
              frame.pose().getRotation().getRadians() - previous.pose().getRotation().getRadians());
      previous = frame;
    }
    return Math.toDegrees(total);
  }

  /** How far the robot's heading is from facing the hub, in degrees. */
  private static double aimError(Frame f) {
    Rotation2d wanted = FieldConstants.HUB.minus(f.pose().getTranslation()).getAngle();
    return Math.toDegrees(MathUtil.angleModulus(wanted.minus(f.pose().getRotation()).getRadians()));
  }

  private static boolean near(double value, double target) {
    return Math.abs(value - target) < 1;
  }

  private static boolean intakeIdle(Frame f) {
    return f.rollers() == 0 && !f.deployRequested();
  }

  private static boolean intakeAndIndexerIdle(Frame f) {
    return intakeIdle(f) && f.indexer() == 0;
  }

  private static boolean allIdle(Frame f) {
    return intakeAndIndexerIdle(f) && f.targetRpm() == 0;
  }

  private static boolean allianceColor(Frame f) {
    String led = f.ledName();
    return f.alliance().isEmpty()
        ? led.equals("red") || led.equals("blue")
        : led.equals(f.alliance());
  }

  private static String allianceNote(Frame f) {
    return f.alliance().isEmpty() ? "" : " (you're on the " + f.alliance() + " alliance)";
  }

  private static String shooterState(Frame f) {
    return f.targetRpm() == 0
        ? f("the shooter wasn't asked to spin (%.0f RPM)", f.rpm())
        : f("the shooter was asked for %.0f RPM and spinning at %.0f", f.targetRpm(), f.rpm());
  }

  private static String intakeState(Frame f) {
    String where = f.intakeOut() ? "out" : f.intakeIn() ? "in" : "moving";
    return f("the intake was %s, rollers at %.1f", where, f.rollers());
  }

  private static String everything(Frame f) {
    return intakeState(f)
        + f(", indexer at %.1f, shooter asked for %.0f RPM", f.indexer(), f.targetRpm());
  }

  private static String climberState(Frame f) {
    return f(
        "the climber was at %.2f m with %.1f V%s",
        f.climberHeight(),
        f.climberVolts(),
        f.hang() > 0 ? f(", robot %.0f%% lifted", f.hang() * 100) : "");
  }

  private static String describe(List<String> requirements) {
    return requirements.isEmpty() ? "no subsystems" : String.join(" and ", requirements);
  }
}
