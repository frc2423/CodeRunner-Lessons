package frc.robot.lab;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.auto.NamedCommands;
import com.pathplanner.lib.path.GoalEndState;
import com.pathplanner.lib.path.PathConstraints;
import com.pathplanner.lib.path.PathPlannerPath;
import edu.wpi.first.hal.AllianceStationID;
import edu.wpi.first.hal.HAL;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.simulation.DriverStationSim;
import edu.wpi.first.wpilibj.simulation.SimHooks;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.challenges.Challenge00Example;
import frc.robot.subsystems.ClimberMotor;
import frc.robot.subsystems.Drive;
import frc.robot.subsystems.Indexer;
import frc.robot.subsystems.Intake;
import frc.robot.subsystems.Shooter;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Runs reference solutions (and deliberately naive attempts) through every challenge's real trial
 * and grading code. Every "reference" line should say OK; the naive ones should say NOPE.
 *
 * <p>See ../README.md for how to run it. Kept out of the module so students don't see solutions.
 */
class CalibrationTest {
  static CommandLab lab;

  @BeforeAll
  static void setUp() {
    HAL.initialize(500, 0);
    SimHooks.pauseTiming();
    DriverStationSim.setDsAttached(true);
    DriverStationSim.setAllianceStationId(AllianceStationID.Blue1);
    DriverStationSim.setEnabled(false);
    DriverStationSim.notifyNewData();
    DriverStation.refreshData();
    lab = new CommandLab();
  }

  static void loop() {
    DriverStationSim.notifyNewData();
    DriverStation.refreshData();
    lab.periodic();
    SimHooks.stepTiming(World.DT);
  }

  static boolean run(String id, String name, Supplier<CommandChallenge> solution) {
    Challenges.Spec spec = Challenges.get(id);
    DriverStationSim.setEnabled(false);
    for (int i = 0; i < 3; i++) {
      loop();
    }
    lab.start(id, CommandLab.Mode.TRIAL, id + "|trial#" + System.nanoTime(), solution);
    loop();
    DriverStationSim.setAutonomous(spec.script().mode == Script.Mode.AUTO);
    DriverStationSim.setEnabled(true);
    int limit = (int) (spec.script().duration / World.DT) + 200;
    for (int i = 0; i < limit && lab.phase() != CommandLab.Phase.DONE; i++) {
      loop();
      if (!lab.error().isEmpty()) {
        break;
      }
    }
    DriverStationSim.setEnabled(false);
    loop();

    StringBuilder out = new StringBuilder();
    CommandLab.TrialResult result = lab.lastResult();
    boolean passed = result != null && result.passed();
    if (result == null) {
      out.append("    ERROR: ").append(lab.error()).append('\n');
    } else {
      for (int i = 0; i < spec.checks().size(); i++) {
        Check check = spec.checks().get(i);
        Check.Outcome o = result.outcomes().get(i);
        out.append(
            String.format(
                "    %s %s%s: %s%n",
                o.pass() ? "PASS" : "FAIL", check.bonus ? "(bonus) " : "", check.label, o.detail()));
      }
    }
    System.out.printf(
        "%s [%s] %s%s%n%s",
        passed ? "OK  " : "NOPE",
        id,
        name,
        result != null ? " ★" + result.stars() : "",
        out);
    return passed;
  }

  // Shared pieces of the solutions ---------------------------------------------------------

  abstract static class Solution extends CommandChallenge {
    Command intakeUntilLoaded() {
      return Commands.runEnd(
              () -> {
                intake.deploy();
                intake.setRollers(Intake.INTAKE_SPEED);
                indexer.setSpeed(Indexer.LOAD_SPEED);
              },
              () -> {
                intake.stopRollers();
                intake.retract();
                indexer.stop();
              },
              intake,
              indexer)
          .until(indexer::hasPiece)
          .withName("Intake");
    }

    Command feed() {
      return indexer
          .runEnd(() -> indexer.setSpeed(Indexer.FEED_SPEED), indexer::stop)
          .until(() -> !indexer.hasPiece())
          .withName("Feed");
    }

    Command shoot() {
      return Commands.sequence(
              shooter.runOnce(() -> shooter.setRPM(Shooter.SHOT_RPM)),
              Commands.waitUntil(shooter::atSpeed),
              feed(),
              Commands.waitSeconds(0.2))
          .finallyDo(
              () -> {
                shooter.stop();
                indexer.stop();
              })
          .withName("Shoot");
    }

    Command spinUp() {
      return shooter
          .startEnd(() -> shooter.setRPM(Shooter.SHOT_RPM), shooter::stop)
          .withName("Spin up");
    }

    Command stickDrive() {
      return drive
          .run(
              () ->
                  drive.driveFieldRelative(
                      -MathUtil.applyDeadband(controller.getLeftY(), 0.1) * Drive.MAX_SPEED,
                      -MathUtil.applyDeadband(controller.getLeftX(), 0.1) * Drive.MAX_SPEED,
                      -MathUtil.applyDeadband(controller.getRightX(), 0.1)
                          * Drive.MAX_ANGULAR_SPEED))
          .withName("Drive with sticks");
    }

    @Override
    public void configureBindings() {}
  }

  static Supplier<CommandChallenge> bindings(java.util.function.Consumer<Solution> body) {
    return () ->
        new Solution() {
          @Override
          public void configureBindings() {
            body.accept(this);
          }
        };
  }

  static class SolutionClimber extends SubsystemBase {
    final ClimberMotor motor = new ClimberMotor();
    final boolean safe;

    SolutionClimber(boolean safe) {
      this.safe = safe;
      setName("Climber");
    }

    Command extend() {
      Command up = run(() -> motor.setVoltage(8));
      if (safe) {
        up = up.until(() -> motor.getHeight() >= 0.58);
      }
      return up.finallyDo(motor::stop).withName("Extend");
    }

    Command climb() {
      return run(() -> motor.setVoltage(-10))
          .until(motor::isAtBottom)
          .finallyDo(motor::stop)
          .withName("Climb");
    }
  }

  static class SolutionAim extends Command {
    final Drive drive;
    final DoubleSupplier x;
    final DoubleSupplier y;
    final PIDController pid;

    SolutionAim(Drive drive, DoubleSupplier x, DoubleSupplier y, double kP, double kD) {
      this.drive = drive;
      this.x = x;
      this.y = y;
      pid = new PIDController(kP, 0, kD);
      pid.enableContinuousInput(-Math.PI, Math.PI);
      addRequirements(drive);
      setName("Aim at hub");
    }

    @Override
    public void initialize() {
      pid.reset();
    }

    @Override
    public void execute() {
      double turn =
          pid.calculate(drive.getRotation().getRadians(), drive.getAngleToHub().getRadians());
      drive.driveFieldRelative(
          x.getAsDouble(),
          y.getAsDouble(),
          MathUtil.clamp(turn, -Drive.MAX_ANGULAR_SPEED, Drive.MAX_ANGULAR_SPEED));
    }

    @Override
    public void end(boolean interrupted) {
      drive.stop();
    }
  }

  // ---------------------------------------------------------------------------------------------

  @Test
  void referenceSolutions() {
    int failed = 0;
    failed += run("example", "the example as shipped", Challenge00Example::new) ? 0 : 1;

    failed +=
        run(
                "spin-up",
                "runOnce on A and B",
                bindings(
                    s -> {
                      s.controller
                          .a()
                          .onTrue(
                              s.shooter
                                  .runOnce(() -> s.shooter.setRPM(Shooter.SHOT_RPM))
                                  .withName("Spin up"));
                      s.controller.b().onTrue(s.shooter.runOnce(s.shooter::stop).withName("Stop"));
                    }))
            ? 0
            : 1;

    failed +=
        run(
                "hold-to-intake",
                "startEnd while LB",
                bindings(
                    s ->
                        s.controller
                            .leftBumper()
                            .whileTrue(
                                s.intake
                                    .startEnd(
                                        () -> {
                                          s.intake.deploy();
                                          s.intake.setRollers(Intake.INTAKE_SPEED);
                                        },
                                        () -> {
                                          s.intake.stopRollers();
                                          s.intake.retract();
                                        })
                                    .withName("Intake"))))
            ? 0
            : 1;

    failed +=
        run(
                "toggle",
                "toggleOnTrue + X stops",
                bindings(
                    s -> {
                      s.controller.y().toggleOnTrue(s.spinUp());
                      s.controller.x().onTrue(s.shooter.runOnce(s.shooter::stop));
                    }))
            ? 0
            : 1;

    failed +=
        run(
                "drive-with-sticks",
                "field-relative with deadband",
                bindings(s -> s.drive.setDefaultCommand(s.stickDrive())))
            ? 0
            : 1;

    failed +=
        run(
                "intake-until-loaded",
                "runEnd(intake, indexer).until(hasPiece).unless(hasPiece)",
                bindings(
                    s ->
                        s.controller
                            .leftBumper()
                            .whileTrue(s.intakeUntilLoaded().unless(s.indexer::hasPiece))))
            ? 0
            : 1;

    failed +=
        run(
                "shoot",
                "sequence with waitUntil",
                bindings(s -> s.controller.rightTrigger().onTrue(s.shoot())))
            ? 0
            : 1;

    failed +=
        run(
                "ready-set-shoot",
                "sequence deadlineFor spin-up",
                bindings(
                    s ->
                        s.controller
                            .rightBumper()
                            .whileTrue(
                                Commands.sequence(
                                        s.intakeUntilLoaded(),
                                        Commands.waitUntil(s.shooter::atSpeed),
                                        s.feed(),
                                        Commands.waitSeconds(0.2))
                                    .deadlineFor(s.spinUp())
                                    .withName("Intake and shoot"))))
            ? 0
            : 1;

    failed +=
        run(
                "whos-in-charge",
                "kCancelIncoming + finallyDo + cancel()",
                bindings(
                    s -> {
                      Command shoot =
                          s.shoot()
                              .withInterruptBehavior(Command.InterruptionBehavior.kCancelIncoming);
                      s.controller.rightTrigger().onTrue(shoot);
                      s.controller.leftBumper().whileTrue(s.intakeUntilLoaded());
                      s.controller.b().onTrue(Commands.runOnce(shoot::cancel));
                    }))
            ? 0
            : 1;

    failed +=
        run(
                "led-signals",
                "default + and(negate) + rumble",
                bindings(
                    s -> {
                      s.controller.leftBumper().whileTrue(s.intakeUntilLoaded());
                      s.controller.a().onTrue(s.shooter.runOnce(() -> s.shooter.setRPM(3000)));
                      s.controller.b().onTrue(s.shooter.runOnce(s.shooter::stop));
                      s.leds.setDefaultCommand(
                          s.leds.run(
                              () ->
                                  s.leds.setColor(
                                      DriverStation.getAlliance()
                                              .orElse(DriverStation.Alliance.Blue)
                                          == DriverStation.Alliance.Red
                                          ? Color.kRed
                                          : Color.kBlue)));
                      Trigger hasPiece = new Trigger(s.indexer::hasPiece);
                      Trigger atSpeed = new Trigger(s.shooter::atSpeed);
                      hasPiece
                          .and(atSpeed.negate())
                          .whileTrue(s.leds.run(() -> s.leds.setColor(Color.kGreen)));
                      atSpeed.whileTrue(s.leds.run(() -> s.leds.setColor(Color.kWhite)));
                      hasPiece.onTrue(
                          Commands.startEnd(
                                  () -> s.controller.setRumble(RumbleType.kBothRumble, 1),
                                  () -> s.controller.setRumble(RumbleType.kBothRumble, 0))
                              .withTimeout(0.5));
                    }))
            ? 0
            : 1;

    failed +=
        run(
                "smart-triggers",
                "and/negate/debounce",
                bindings(
                    s -> {
                      Trigger loaded = new Trigger(s.indexer::hasPiece);
                      s.controller.leftBumper().whileTrue(s.intakeUntilLoaded());
                      s.controller.rightTrigger().and(loaded).onTrue(s.shoot());
                      s.controller
                          .rightTrigger()
                          .and(loaded.negate())
                          .onTrue(
                              s.leds
                                  .startEnd(() -> s.leds.setColor(Color.kRed), s.leds::off)
                                  .withTimeout(0.5));
                      new Trigger(() -> s.intake.getRollerCurrent() > 30)
                          .debounce(0.25)
                          .onTrue(
                              s.intake
                                  .startEnd(
                                      () -> s.intake.setRollers(Intake.EJECT_SPEED),
                                      s.intake::stopRollers)
                                  .withTimeout(0.3));
                    }))
            ? 0
            : 1;

    failed +=
        run(
                "climber",
                "until(TOP) / until(isAtBottom)",
                bindings(
                    s -> {
                      SolutionClimber climber = new SolutionClimber(true);
                      s.controller.povUp().onTrue(climber.extend());
                      s.controller.povDown().onTrue(climber.climb());
                    }))
            ? 0
            : 1;

    failed +=
        run(
                "aim-at-hub",
                "PID kP=12 kD=0.8",
                bindings(
                    s -> {
                      s.drive.setDefaultCommand(s.stickDrive());
                      s.controller
                          .leftTrigger()
                          .whileTrue(
                              new SolutionAim(
                                  s.drive,
                                  () -> -s.controller.getLeftY() * Drive.MAX_SPEED,
                                  () -> -s.controller.getLeftX() * Drive.MAX_SPEED,
                                  12,
                                  0.8));
                    }))
            ? 0
            : 1;

    failed += run("timed-auto", "shoot, grab, come back, shoot", CalibrationTest::timedAuto) ? 0 : 1;
    failed += run("pathplanner-auto", "three piece from paths", CalibrationTest::pathAuto) ? 0 : 1;

    System.out.println(failed == 0 ? "ALL REFERENCE SOLUTIONS PASS" : failed + " FAILED");
  }

  static CommandChallenge timedAuto() {
    return new Solution() {
      @Override
      public Command getAutonomousCommand() {
        Command forward =
            drive.run(() -> drive.driveRobotRelative(1.0, 0, 0)).withTimeout(1.3);
        Command back = drive.run(() -> drive.driveRobotRelative(-1.0, 0, 0)).withTimeout(1.3);
        return Commands.sequence(
            shoot(),
            Commands.deadline(forward, intakeUntilLoaded()),
            drive.runOnce(drive::stop),
            Commands.waitSeconds(0.3),
            back,
            drive.runOnce(drive::stop),
            Commands.waitSeconds(0.3),
            shoot());
      }
    };
  }

  static CommandChallenge pathAuto() {
    return new Solution() {
      {
        NamedCommands.registerCommand("Intake", intakeUntilLoaded().withTimeout(3));
        NamedCommands.registerCommand("Shoot", shoot());
      }

      @Override
      public Command getAutonomousCommand() {
        try {
          PathPlannerPath toTop = PathPlannerPath.fromPathFile("Start to Top Piece");
          PathPlannerPath fromTop = PathPlannerPath.fromPathFile("Top Piece to Shooting Spot");
          PathConstraints constraints = new PathConstraints(3.0, 3.0, 9.4, 12.5);
          PathPlannerPath toBottom =
              new PathPlannerPath(
                  PathPlannerPath.waypointsFromPoses(
                      new Pose2d(1.626, 4.035, Rotation2d.kZero),
                      new Pose2d(2.9, 2.72, Rotation2d.fromDegrees(-90))),
                  constraints,
                  null,
                  new GoalEndState(0, Rotation2d.fromDegrees(-90)));
          PathPlannerPath fromBottom =
              new PathPlannerPath(
                  PathPlannerPath.waypointsFromPoses(
                      new Pose2d(2.9, 2.72, Rotation2d.fromDegrees(90)),
                      new Pose2d(1.626, 4.035, Rotation2d.fromDegrees(180))),
                  constraints,
                  null,
                  new GoalEndState(0, Rotation2d.kZero));
          return Commands.sequence(
              AutoBuilder.resetOdom(new Pose2d(1.626, 4.035, Rotation2d.kZero)),
              NamedCommands.getCommand("Shoot"),
              Commands.deadline(AutoBuilder.followPath(toTop), intakeUntilLoaded()),
              AutoBuilder.followPath(fromTop),
              shoot(),
              Commands.deadline(AutoBuilder.followPath(toBottom), intakeUntilLoaded()),
              AutoBuilder.followPath(fromBottom),
              shoot());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  // Naive attempts: these should fail ----------------------------------------------------------

  @Test
  void naiveAttempts() {
    run(
        "spin-up",
        "NAIVE Commands.runOnce without the shooter requirement",
        bindings(
            s -> {
              s.controller.a().onTrue(Commands.runOnce(() -> s.shooter.setRPM(3000)));
              s.controller.b().onTrue(Commands.runOnce(s.shooter::stop));
            }));
    run(
        "spin-up",
        "NAIVE whileTrue(startEnd)",
        bindings(s -> s.controller.a().whileTrue(s.spinUp())));
    run(
        "hold-to-intake",
        "NAIVE onTrue instead of whileTrue",
        bindings(
            s ->
                s.controller
                    .leftBumper()
                    .onTrue(
                        s.intake.startEnd(
                            () -> {
                              s.intake.deploy();
                              s.intake.setRollers(1);
                            },
                            () -> {
                              s.intake.stopRollers();
                              s.intake.retract();
                            }))));
    run("toggle", "NAIVE onTrue(startEnd)", bindings(s -> s.controller.y().onTrue(s.spinUp())));
    run(
        "drive-with-sticks",
        "NAIVE robot-relative, no deadband (bonus should fail)",
        bindings(
            s ->
                s.drive.setDefaultCommand(
                    s.drive.run(
                        () ->
                            s.drive.driveRobotRelative(
                                -s.controller.getLeftY() * Drive.MAX_SPEED,
                                -s.controller.getLeftX() * Drive.MAX_SPEED,
                                -s.controller.getRightX() * Drive.MAX_ANGULAR_SPEED)))));
    run(
        "drive-with-sticks",
        "NAIVE reads the sticks once",
        bindings(
            s -> {
              double x = -s.controller.getLeftY() * Drive.MAX_SPEED;
              double y = -s.controller.getLeftX() * Drive.MAX_SPEED;
              s.drive.setDefaultCommand(s.drive.run(() -> s.drive.driveFieldRelative(x, y, 0)));
            }));
    run(
        "intake-until-loaded",
        "NAIVE no until(hasPiece)",
        bindings(
            s ->
                s.controller
                    .leftBumper()
                    .whileTrue(
                        Commands.runEnd(
                            () -> {
                              s.intake.deploy();
                              s.intake.setRollers(1);
                              s.indexer.setSpeed(1);
                            },
                            () -> {
                              s.intake.stopRollers();
                              s.intake.retract();
                              s.indexer.stop();
                            },
                            s.intake,
                            s.indexer))));
    run(
        "intake-until-loaded",
        "NAIVE intake only, no indexer",
        bindings(
            s ->
                s.controller
                    .leftBumper()
                    .whileTrue(
                        s.intake
                            .runEnd(
                                () -> {
                                  s.intake.deploy();
                                  s.intake.setRollers(1);
                                },
                                () -> {
                                  s.intake.stopRollers();
                                  s.intake.retract();
                                })
                            .until(s.indexer::hasPiece))));
    run(
        "shoot",
        "NAIVE no waitUntil(atSpeed)",
        bindings(
            s ->
                s.controller
                    .rightTrigger()
                    .onTrue(
                        Commands.sequence(
                            s.shooter.runOnce(() -> s.shooter.setRPM(3000)),
                            s.feed(),
                            s.shooter.runOnce(s.shooter::stop)))));
    run(
        "ready-set-shoot",
        "NAIVE one thing after another",
        bindings(
            s ->
                s.controller
                    .rightBumper()
                    .whileTrue(Commands.sequence(s.intakeUntilLoaded(), s.shoot()))));
    run(
        "whos-in-charge",
        "NAIVE the scaffold as shipped",
        bindings(
            s -> {
              Command shoot =
                  Commands.sequence(
                      s.shooter.runOnce(() -> s.shooter.setRPM(3000)),
                      Commands.waitUntil(s.shooter::atSpeed),
                      s.feed(),
                      Commands.waitSeconds(0.2),
                      s.shooter.runOnce(s.shooter::stop));
              s.controller.rightTrigger().onTrue(shoot);
              s.controller.leftBumper().whileTrue(s.intakeUntilLoaded());
            }));
    run(
        "whos-in-charge",
        "NAIVE kCancelIncoming but no finallyDo",
        bindings(
            s -> {
              Command shoot =
                  Commands.sequence(
                          s.shooter.runOnce(() -> s.shooter.setRPM(3000)),
                          Commands.waitUntil(s.shooter::atSpeed),
                          s.feed(),
                          Commands.waitSeconds(0.2),
                          s.shooter.runOnce(s.shooter::stop))
                      .withInterruptBehavior(Command.InterruptionBehavior.kCancelIncoming);
              s.controller.rightTrigger().onTrue(shoot);
              s.controller.leftBumper().whileTrue(s.intakeUntilLoaded());
              s.controller.b().onTrue(Commands.runOnce(shoot::cancel));
            }));
    run(
        "led-signals",
        "NAIVE two whileTrue triggers",
        bindings(
            s -> {
              s.controller.leftBumper().whileTrue(s.intakeUntilLoaded());
              s.controller.a().onTrue(s.shooter.runOnce(() -> s.shooter.setRPM(3000)));
              s.controller.b().onTrue(s.shooter.runOnce(s.shooter::stop));
              s.leds.setDefaultCommand(s.leds.run(() -> s.leds.setColor(Color.kBlue)));
              new Trigger(s.indexer::hasPiece)
                  .whileTrue(s.leds.run(() -> s.leds.setColor(Color.kGreen)));
              new Trigger(s.shooter::atSpeed)
                  .whileTrue(s.leds.run(() -> s.leds.setColor(Color.kWhite)));
            }));
    run(
        "smart-triggers",
        "NAIVE no debounce",
        bindings(
            s -> {
              Trigger loaded = new Trigger(s.indexer::hasPiece);
              s.controller.leftBumper().whileTrue(s.intakeUntilLoaded());
              s.controller.rightTrigger().and(loaded).onTrue(s.shoot());
              new Trigger(() -> s.intake.getRollerCurrent() > 30)
                  .onTrue(
                      s.intake
                          .startEnd(() -> s.intake.setRollers(-1), s.intake::stopRollers)
                          .withTimeout(0.3));
            }));
    run(
        "climber",
        "NAIVE extend never stops",
        bindings(
            s -> {
              SolutionClimber climber = new SolutionClimber(false);
              s.controller.povUp().onTrue(climber.extend());
              s.controller.povDown().onTrue(climber.climb());
            }));
    run("pathplanner-auto", "NAIVE the shipped scaffold", frc.robot.challenges.Challenge14PathPlannerAuto::new);
  }
}
