package frc.robot.lab;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import java.util.ArrayList;
import java.util.List;

/**
 * The simulated robot: a flywheel, a feeder with a beam break, a drivetrain, a gyro and a pivot. It
 * moves one fixed step per robot loop, so a trial goes the same way every time.
 *
 * <p>Robot code never sees this class. It talks to {@link Hardware}, the lab's stand-in for motor
 * controller and sensor libraries.
 */
final class World {
  static final double DT = 0.02;

  /** How many gyro samples arrive per robot loop (250 Hz). */
  static final int GYRO_SAMPLES_PER_LOOP = 5;

  private static final double FLYWHEEL_KV = 12.0 / (6000.0 * 2.0 * Math.PI / 60.0);
  private static final double FLYWHEEL_TAU = 0.4;
  private static final double FEED_METERS_PER_VOLT_SECOND = 0.15;
  private static final double FEED_LENGTH = 0.3;
  private static final double SHOT_SPEED_LOSS = 0.88;
  private static final double DRIVE_TAU = 0.1;
  private static final double PIVOT_MAX_RAD = 2.2;
  private static final double PIVOT_RAD_PER_SEC_AT_12V = 2.5;
  private static final double PIVOT_TAU = 0.15;

  /** A piece leaving the shooter. */
  record Shot(double time, double rpm) {}

  /** What the robot was really doing, for grading. */
  record State(
      double flywheelRpm,
      double flywheelVolts,
      double feederVolts,
      boolean hasPiece,
      int shots,
      Pose2d pose,
      double yawRad,
      double pivotRad) {}

  private double time = 0;

  private double flywheelVolts = 0;
  private double flywheelRadPerSec = 0;

  private double feederVolts = 0;
  private boolean pieceInFeeder = false;
  private double piecePosition = 0;
  private int piecesWaiting = 0;
  private final List<Shot> shots = new ArrayList<>();

  private double vxCommand = 0;
  private double vyCommand = 0;
  private double omegaCommand = 0;
  private double vx = 0;
  private double vy = 0;
  private double omega = 0;
  private double x = 0;
  private double y = 0;
  private double yaw = 0;

  private double bumpRate = 0;
  private double bumpTimeLeft = 0;
  private double[] gyroRates = new double[0];
  private double[] gyroTimes = new double[0];

  private double pivotVolts = 0;
  private double pivotRad = 0;
  private double pivotRadPerSec = 0;

  /** Puts everything back for a new run. */
  void reset(Pose2d start) {
    time = 0;
    flywheelVolts = 0;
    flywheelRadPerSec = 0;
    feederVolts = 0;
    pieceInFeeder = false;
    piecePosition = 0;
    piecesWaiting = 0;
    shots.clear();
    vxCommand = 0;
    vyCommand = 0;
    omegaCommand = 0;
    vx = 0;
    vy = 0;
    omega = 0;
    x = start.getX();
    y = start.getY();
    yaw = start.getRotation().getRadians();
    bumpRate = 0;
    bumpTimeLeft = 0;
    gyroRates = new double[0];
    gyroTimes = new double[0];
    pivotVolts = 0;
    pivotRad = 0;
    pivotRadPerSec = 0;
  }

  /** Moves everything forward by one robot loop. */
  void step() {
    stepFlywheel();
    stepFeeder();
    stepDrive();
    stepPivot();
    time += DT;
  }

  private void stepFlywheel() {
    double acceleration =
        (flywheelVolts - FLYWHEEL_KV * flywheelRadPerSec) / (FLYWHEEL_KV * FLYWHEEL_TAU);
    flywheelRadPerSec += acceleration * DT;
  }

  private void stepFeeder() {
    if (!pieceInFeeder && piecesWaiting > 0) {
      piecesWaiting--;
      pieceInFeeder = true;
      piecePosition = 0;
    }
    if (!pieceInFeeder) {
      return;
    }
    piecePosition += Math.max(feederVolts, 0) * FEED_METERS_PER_VOLT_SECOND * DT;
    if (piecePosition >= FEED_LENGTH) {
      shots.add(new Shot(time, flywheelRpm()));
      flywheelRadPerSec *= SHOT_SPEED_LOSS;
      pieceInFeeder = false;
      piecePosition = 0;
    }
  }

  private void stepDrive() {
    double blend = DT / DRIVE_TAU;
    vx += (vxCommand - vx) * blend;
    vy += (vyCommand - vy) * blend;
    omega += (omegaCommand - omega) * blend;
    x += vx * DT;
    y += vy * DT;

    // The gyro sees the robot's motion five times per loop, so it catches quick bumps that are over
    // before the robot code's next loop.
    double substep = DT / GYRO_SAMPLES_PER_LOOP;
    gyroRates = new double[GYRO_SAMPLES_PER_LOOP];
    gyroTimes = new double[GYRO_SAMPLES_PER_LOOP];
    for (int i = 0; i < GYRO_SAMPLES_PER_LOOP; i++) {
      double rate = omega;
      if (bumpTimeLeft > 1e-9) {
        rate += bumpRate;
        bumpTimeLeft -= substep;
      }
      yaw += rate * substep;
      gyroRates[i] = rate;
      gyroTimes[i] = time + (i + 1) * substep;
    }
  }

  private void stepPivot() {
    double speedTarget = pivotVolts / 12.0 * PIVOT_RAD_PER_SEC_AT_12V;
    pivotRadPerSec += (speedTarget - pivotRadPerSec) * (DT / PIVOT_TAU);
    pivotRad += pivotRadPerSec * DT;
    if (pivotRad <= 0 || pivotRad >= PIVOT_MAX_RAD) {
      pivotRad = MathUtil.clamp(pivotRad, 0, PIVOT_MAX_RAD);
      pivotRadPerSec = 0;
    }
  }

  State state() {
    return new State(
        flywheelRpm(),
        flywheelVolts,
        feederVolts,
        pieceInFeeder,
        shots.size(),
        pose(),
        yaw,
        pivotRad);
  }

  double time() {
    return time;
  }

  double flywheelRpm() {
    return flywheelRadPerSec * 60.0 / (2.0 * Math.PI);
  }

  List<Shot> shots() {
    return shots;
  }

  /** Puts a game piece in the feeder (or in line for it). */
  void loadPiece() {
    if (pieceInFeeder) {
      piecesWaiting++;
    } else {
      pieceInFeeder = true;
      piecePosition = 0;
    }
  }

  /** The robot gets hit: for {@code seconds}, the gyro reads an extra {@code rate} rad/s. */
  void bump(double rate, double seconds) {
    bumpRate = rate;
    bumpTimeLeft = seconds;
  }

  // What Hardware reads and writes --------------------------------------------------------------

  void setFlywheelVolts(double volts) {
    flywheelVolts = MathUtil.clamp(volts, -12, 12);
  }

  double flywheelRadPerSec() {
    return flywheelRadPerSec;
  }

  double flywheelVolts() {
    return flywheelVolts;
  }

  double flywheelAmps() {
    return Math.abs(flywheelVolts - FLYWHEEL_KV * flywheelRadPerSec) / 0.15;
  }

  void setFeederVolts(double volts) {
    feederVolts = MathUtil.clamp(volts, -12, 12);
  }

  double feederVolts() {
    return feederVolts;
  }

  double feederAmps() {
    return Math.abs(feederVolts) * (pieceInFeeder ? 2.5 : 0.8);
  }

  boolean hasPiece() {
    return pieceInFeeder;
  }

  void setSpeeds(double vx, double vy, double omega) {
    vxCommand = vx;
    vyCommand = vy;
    omegaCommand = omega;
  }

  Pose2d pose() {
    return new Pose2d(new Translation2d(x, y), new Rotation2d(yaw));
  }

  double yawRad() {
    return yaw;
  }

  double[] gyroRates() {
    return gyroRates;
  }

  double[] gyroTimes() {
    return gyroTimes;
  }

  void setPivotVolts(double volts) {
    pivotVolts = MathUtil.clamp(volts, -12, 12);
  }

  double pivotVolts() {
    return pivotVolts;
  }

  double pivotRad() {
    return pivotRad;
  }

  double pivotRadPerSec() {
    return pivotRadPerSec;
  }
}
