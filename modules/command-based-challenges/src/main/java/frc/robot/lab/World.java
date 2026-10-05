package frc.robot.lab;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.util.Color;
import frc.robot.FieldConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The simulated robot and field: where the robot is, what its motors are doing, where the game
 * pieces are, and the score. The subsystems talk to it the way real subsystems talk to motor
 * controllers and sensors.
 *
 * <p>You don't need to read or edit this file.
 */
public final class World {
  public static final double DT = 0.02;

  // Drivetrain ---------------------------------------------------------------------------------
  public static final double MAX_SPEED = 4.0;
  public static final double MAX_ANGULAR_SPEED = 2 * Math.PI;
  static final double MAX_ACCEL = 8.0;
  static final double MAX_ANGULAR_ACCEL = 25.0;
  /** Half the robot's bumper-to-bumper width: it is a 0.8 m square. */
  static final double HALF_SIZE = 0.4;

  private Pose2d pose = FieldConstants.SHOOTING_SPOT;
  private double vx;
  private double vy;
  private double omega;
  private ChassisSpeeds commanded = new ChassisSpeeds();
  /** Odometry is perfect here, but resetting it can still put it somewhere the robot isn't. */
  private Rotation2d odomRotation = Rotation2d.kZero;

  private Translation2d odomShift = Translation2d.kZero;

  // Intake and indexer -----------------------------------------------------------------------
  /** Seconds the intake takes to swing out or in. */
  static final double DEPLOY_TIME = 0.25;

  /** How fast a piece moves through the robot at full roller speed, in "piece units" per second. */
  static final double FEED_RATE = 3.0;

  /** Where along the path through the robot the beam break sees a piece. */
  static final double BEAM_BREAK_START = 0.9;

  /** A piece this far along has reached the shooter wheels (and left the beam break). */
  static final double SHOOTER_ENTRY = 1.4;

  /** The intake rollers can push a piece this far; the indexer has to take it from here. */
  static final double HANDOFF = 0.5;

  private boolean deployRequested;
  private double deploy;
  private double rollers;
  private double indexer;
  /** Whether there is a piece in the robot. */
  private boolean loaded;

  /** How far the piece in the robot has travelled: 0 is in the intake, negative is backing out. */
  private double piece;

  private boolean jammed;
  private boolean jamNext;
  private double reverseTime;
  private double spikeTime;

  // Shooter ----------------------------------------------------------------------------------
  private double rpm;
  private double targetRpm;

  // Climber ----------------------------------------------------------------------------------
  public static final double CLIMBER_TOP = 0.6;
  static final double CLIMBER_SPEED = 0.6;
  static final double HOOK_HEIGHT = 0.55;
  private double climberHeight;
  private double climberVolts;
  private boolean hooked;
  private double hang;
  private double strain;

  // LEDs ---------------------------------------------------------------------------------------
  private Color led = Color.kBlack;

  // Field --------------------------------------------------------------------------------------
  record Shot(
      int id,
      double x0,
      double y0,
      double x1,
      double y1,
      double launched,
      double flight,
      int outcome,
      double rpm) {
    static final int SCORED = 1;
    static final int MISSED = 0;
    static final int DRIBBLED = -1;

    boolean landed(double time) {
      return time >= launched + flight;
    }
  }

  private final List<Translation2d> pieces = new ArrayList<>();
  private final List<Shot> shots = new ArrayList<>();
  private int shotCount;
  private int score;
  private int misses;
  private int dribbles;
  private int pickups;
  private double time;
  private boolean humanPlayer;
  private double nextSpawn;
  private Random random = new Random(1);
  private int resets = 0;

  /** Puts everything back as the script describes. */
  void reset(Script script) {
    resets++;
    pose = script.start;
    vx = vy = omega = 0;
    commanded = new ChassisSpeeds();
    odomRotation = Rotation2d.kZero;
    odomShift = Translation2d.kZero;
    deployRequested = false;
    deploy = 0;
    rollers = 0;
    indexer = 0;
    loaded = script.preload;
    piece = script.preload ? BEAM_BREAK_START + 0.05 : 0;
    jammed = false;
    jamNext = false;
    reverseTime = 0;
    spikeTime = 0;
    rpm = 0;
    targetRpm = 0;
    climberHeight = 0;
    climberVolts = 0;
    hooked = false;
    hang = 0;
    strain = 0;
    led = Color.kBlack;
    pieces.clear();
    pieces.addAll(script.pieces);
    shots.clear();
    score = misses = dribbles = pickups = 0;
    time = 0;
    humanPlayer = script.humanPlayer;
    nextSpawn = 2.0;
    random = new Random(1);
  }

  /** Moves everything forward one robot loop. */
  void step() {
    stepDrive();
    stepIntake();
    stepShooter();
    stepClimber();
    stepField();
    time += DT;
  }

  // Drivetrain ---------------------------------------------------------------------------------

  private void stepDrive() {
    double heading = pose.getRotation().getRadians();
    double targetVx = 0;
    double targetVy = 0;
    double targetOmega = 0;
    if (hang <= 0) {
      double cos = Math.cos(heading);
      double sin = Math.sin(heading);
      targetVx = commanded.vxMetersPerSecond * cos - commanded.vyMetersPerSecond * sin;
      targetVy = commanded.vxMetersPerSecond * sin + commanded.vyMetersPerSecond * cos;
      double speed = Math.hypot(targetVx, targetVy);
      if (speed > MAX_SPEED) {
        targetVx *= MAX_SPEED / speed;
        targetVy *= MAX_SPEED / speed;
      }
      targetOmega =
          MathUtil.clamp(commanded.omegaRadiansPerSecond, -MAX_ANGULAR_SPEED, MAX_ANGULAR_SPEED);
    }

    double dvx = targetVx - vx;
    double dvy = targetVy - vy;
    double dv = Math.hypot(dvx, dvy);
    double maxStep = MAX_ACCEL * DT;
    if (dv > maxStep) {
      dvx *= maxStep / dv;
      dvy *= maxStep / dv;
    }
    vx += dvx;
    vy += dvy;
    omega += MathUtil.clamp(targetOmega - omega, -MAX_ANGULAR_ACCEL * DT, MAX_ANGULAR_ACCEL * DT);

    double x = pose.getX() + vx * DT;
    double y = pose.getY() + vy * DT;
    Rotation2d rotation = pose.getRotation().plus(Rotation2d.fromRadians(omega * DT));

    // Field walls.
    if (x < HALF_SIZE || x > FieldConstants.FIELD_LENGTH - HALF_SIZE) {
      x = MathUtil.clamp(x, HALF_SIZE, FieldConstants.FIELD_LENGTH - HALF_SIZE);
      vx = 0;
    }
    if (y < HALF_SIZE || y > FieldConstants.FIELD_WIDTH - HALF_SIZE) {
      y = MathUtil.clamp(y, HALF_SIZE, FieldConstants.FIELD_WIDTH - HALF_SIZE);
      vy = 0;
    }

    // The hub is solid.
    Translation2d away = new Translation2d(x, y).minus(FieldConstants.HUB);
    double distance = away.getNorm();
    double closest = FieldConstants.HUB_RADIUS + HALF_SIZE;
    if (distance < closest) {
      Translation2d normal =
          distance < 1e-6 ? new Translation2d(-1, 0) : away.div(distance); // straight back out
      x = FieldConstants.HUB.getX() + normal.getX() * closest;
      y = FieldConstants.HUB.getY() + normal.getY() * closest;
      double inward = vx * normal.getX() + vy * normal.getY();
      if (inward < 0) {
        vx -= inward * normal.getX();
        vy -= inward * normal.getY();
      }
    }

    pose = new Pose2d(x, y, rotation);
  }

  void setDriveSpeeds(ChassisSpeeds robotRelative) {
    commanded = robotRelative == null ? new ChassisSpeeds() : robotRelative;
  }

  /** Where the robot thinks it is (its odometry). */
  Pose2d odometryPose() {
    return new Pose2d(
        pose.getTranslation().rotateBy(odomRotation).plus(odomShift),
        pose.getRotation().plus(odomRotation));
  }

  /** Tells the odometry the robot is at {@code newPose}. The robot itself doesn't move. */
  void resetOdometry(Pose2d newPose) {
    odomRotation = newPose.getRotation().minus(pose.getRotation());
    odomShift = newPose.getTranslation().minus(pose.getTranslation().rotateBy(odomRotation));
  }

  /** Where the robot really is on the field. */
  Pose2d pose() {
    return pose;
  }

  ChassisSpeeds robotRelativeSpeeds() {
    return ChassisSpeeds.fromFieldRelativeSpeeds(vx, vy, omega, pose.getRotation());
  }

  double fieldVx() {
    return vx;
  }

  double fieldVy() {
    return vy;
  }

  double omega() {
    return omega;
  }

  // Intake and indexer -----------------------------------------------------------------------

  private void stepIntake() {
    double deployTarget = deployRequested ? 1 : 0;
    deploy += MathUtil.clamp(deployTarget - deploy, -DT / DEPLOY_TIME, DT / DEPLOY_TIME);
    spikeTime = Math.max(spikeTime - DT, 0);

    if (!loaded) {
      if (deploy >= 0.9 && rollers >= 0.3) {
        for (int i = 0; i < pieces.size(); i++) {
          Translation2d local = toRobot(pieces.get(i));
          if (local.getX() >= HALF_SIZE
              && local.getX() <= HALF_SIZE + 0.45
              && Math.abs(local.getY()) <= 0.35) {
            pieces.remove(i);
            loaded = true;
            piece = 0;
            pickups++;
            spikeTime = 0.12;
            if (jamNext) {
              jammed = true;
              jamNext = false;
            }
            break;
          }
        }
      }
      return;
    }

    if (jammed) {
      if (rollers <= -0.3) {
        reverseTime += DT;
        if (reverseTime >= 0.2) {
          jammed = false;
          reverseTime = 0;
        }
      } else {
        reverseTime = 0;
      }
      return;
    }

    double rate;
    if (piece < HANDOFF) {
      double forward = Math.max(rollers, indexer);
      double backward = Math.min(rollers, indexer);
      if (forward > 0.1) {
        rate = FEED_RATE * forward;
      } else if (backward < -0.1) {
        rate = 0.5 * FEED_RATE * backward;
      } else {
        rate = 0;
      }
      piece += rate * DT;
      if (rate > 0 && piece > HANDOFF && indexer <= 0.1) {
        piece = HANDOFF; // waits for the indexer to take it
      }
    } else {
      rate = FEED_RATE * indexer;
      piece += rate * DT;
    }

    if (piece < -0.5) {
      // Spat back out of the front of the robot.
      loaded = false;
      piece = 0;
      pieces.add(toField(new Translation2d(HALF_SIZE + 0.75, 0)));
    } else if (piece < 0 && rate < 0 && deploy < 0.5) {
      piece = 0; // a stowed intake can't spit it out
    } else if (piece >= SHOOTER_ENTRY) {
      launch();
    }
  }

  void setIntakeDeployed(boolean deployed) {
    deployRequested = deployed;
  }

  void setRollers(double speed) {
    rollers = MathUtil.clamp(speed, -1, 1);
  }

  void setIndexer(double speed) {
    indexer = MathUtil.clamp(speed, -1, 1);
  }

  boolean intakeDeployRequested() {
    return deployRequested;
  }

  double deploy() {
    return deploy;
  }

  double rollers() {
    return rollers;
  }

  double indexer() {
    return indexer;
  }

  /** How far along the piece is, or -1 when the robot is empty. */
  double piece() {
    return loaded ? Math.max(piece, -0.5) : -1;
  }

  boolean hasPiece() {
    return loaded;
  }

  boolean beamBreak() {
    return loaded && piece >= BEAM_BREAK_START;
  }

  boolean jammed() {
    return jammed;
  }

  double rollerCurrent() {
    double current = 4 + 10 * Math.abs(rollers);
    if (spikeTime > 0 && rollers > 0.1) {
      current += 25;
    }
    if (jammed && rollers > 0.1) {
      current += 35;
    }
    return current;
  }

  /** The next piece the intake picks up will jam. */
  void jamNextPiece() {
    jamNext = true;
  }

  // Shooter ----------------------------------------------------------------------------------

  private void stepShooter() {
    double tau = targetRpm <= 0 ? 1.2 : targetRpm >= rpm ? 0.3 : 0.5;
    rpm += (targetRpm - rpm) * (1 - Math.exp(-DT / tau));
    if (Math.abs(rpm) < 1) {
      rpm = 0;
    }
  }

  private void launch() {
    loaded = false;
    piece = 0;
    Rotation2d heading = pose.getRotation();
    Translation2d exit = toField(new Translation2d(0.2, 0));
    int id = ++shotCount;
    if (rpm < 1500) {
      dribbles++;
      Translation2d dropped = toField(new Translation2d(-HALF_SIZE - 0.35, 0));
      shots.add(
          new Shot(
              id,
              exit.getX(),
              exit.getY(),
              dropped.getX(),
              dropped.getY(),
              time,
              0.35,
              Shot.DRIBBLED,
              rpm));
    } else {
      double range = rpm / 1000.0;
      Translation2d landing = exit.plus(new Translation2d(range, heading));
      boolean scored = landing.getDistance(FieldConstants.HUB) <= FieldConstants.HUB_SCORING_RADIUS;
      shots.add(
          new Shot(
              id,
              exit.getX(),
              exit.getY(),
              landing.getX(),
              landing.getY(),
              time,
              0.3 + 0.1 * range,
              scored ? Shot.SCORED : Shot.MISSED,
              rpm));
    }
    rpm *= 0.8; // the piece takes energy out of the wheels
  }

  void setTargetRpm(double target) {
    targetRpm = Math.max(target, 0);
  }

  double rpm() {
    return rpm;
  }

  double targetRpm() {
    return targetRpm;
  }

  // Climber ----------------------------------------------------------------------------------

  private void stepClimber() {
    double speed = climberVolts / 12.0 * CLIMBER_SPEED;
    climberHeight = MathUtil.clamp(climberHeight + speed * DT, 0, CLIMBER_TOP);
    boolean pushingTop = climberHeight >= CLIMBER_TOP - 1e-6 && climberVolts > 1;
    boolean pushingBottom = climberHeight <= 1e-6 && climberVolts < -1;
    strain = pushingTop || pushingBottom ? strain + DT : 0;

    boolean inZone = pose.getTranslation().getDistance(FieldConstants.TOWER) <= towerZone();
    if (climberHeight >= HOOK_HEIGHT) {
      hooked = inZone;
    } else if (!inZone && hang <= 0) {
      hooked = false;
    }
    hang = hooked ? MathUtil.clamp((HOOK_HEIGHT - climberHeight) / (HOOK_HEIGHT - 0.05), 0, 1) : 0;
  }

  private static double towerZone() {
    return FieldConstants.TOWER_ZONE_RADIUS;
  }

  void setClimberVolts(double volts) {
    climberVolts = MathUtil.clamp(volts, -12, 12);
  }

  double climberHeight() {
    return climberHeight;
  }

  double climberVolts() {
    return climberVolts;
  }

  boolean climberAtBottom() {
    return climberHeight <= 0.002;
  }

  /** 0 on the floor, 1 hanging all the way up. */
  double hang() {
    return hang;
  }

  boolean hooked() {
    return hooked;
  }

  /** How long the climber motor has been pushing against a hard stop, in seconds. */
  double strain() {
    return strain;
  }

  // LEDs ---------------------------------------------------------------------------------------

  void setLed(Color color) {
    led = color == null ? Color.kBlack : color;
  }

  Color led() {
    return led;
  }

  // Field --------------------------------------------------------------------------------------

  private void stepField() {
    for (Shot shot : shots) {
      if (shot.landed(time + DT) && !shot.landed(time)) {
        switch (shot.outcome()) {
          case Shot.SCORED -> score++;
          case Shot.MISSED -> {
            misses++;
            pieces.add(onField(new Translation2d(shot.x1(), shot.y1())));
          }
          default -> pieces.add(onField(new Translation2d(shot.x1(), shot.y1())));
        }
      }
    }
    while (shots.size() > 12) {
      shots.remove(0);
    }

    if (humanPlayer && time >= nextSpawn) {
      nextSpawn += 4.0;
      long nearby =
          pieces.stream()
              .filter(p -> p.getDistance(FieldConstants.HUMAN_PLAYER_STATION) < 1.5)
              .count();
      if (nearby < 3) {
        pieces.add(
            FieldConstants.HUMAN_PLAYER_STATION.plus(
                new Translation2d(random.nextDouble() * 0.8, random.nextDouble() * 0.8)));
      }
    }
  }

  /** A landing spot moved off the walls and out of the hub, so the piece can be picked up. */
  private static Translation2d onField(Translation2d spot) {
    double x = MathUtil.clamp(spot.getX(), 0.15, FieldConstants.FIELD_LENGTH - 0.15);
    double y = MathUtil.clamp(spot.getY(), 0.15, FieldConstants.FIELD_WIDTH - 0.15);
    Translation2d moved = new Translation2d(x, y);
    Translation2d away = moved.minus(FieldConstants.HUB);
    if (away.getNorm() < FieldConstants.HUB_RADIUS + 0.15) {
      double angle = away.getNorm() < 1e-6 ? Math.PI : away.getAngle().getRadians();
      moved =
          FieldConstants.HUB.plus(
              new Translation2d(FieldConstants.HUB_RADIUS + 0.15, Rotation2d.fromRadians(angle)));
    }
    return moved;
  }

  /** A human player rolls a piece right into the front of the robot. */
  void rollInPiece() {
    pieces.add(toField(new Translation2d(HALF_SIZE + 0.22, 0)));
  }

  void addPiece(Translation2d where) {
    pieces.add(where);
  }

  List<Translation2d> pieces() {
    return pieces;
  }

  List<Shot> shots() {
    return shots;
  }

  int score() {
    return score;
  }

  int misses() {
    return misses;
  }

  int dribbles() {
    return dribbles;
  }

  int pickups() {
    return pickups;
  }

  double time() {
    return time;
  }

  /** Goes up by one every time the field is reset, so the dashboard can tell runs apart. */
  int resets() {
    return resets;
  }

  private Translation2d toRobot(Translation2d field) {
    return field.minus(pose.getTranslation()).rotateBy(pose.getRotation().unaryMinus());
  }

  private Translation2d toField(Translation2d robot) {
    return robot.rotateBy(pose.getRotation()).plus(pose.getTranslation());
  }
}
