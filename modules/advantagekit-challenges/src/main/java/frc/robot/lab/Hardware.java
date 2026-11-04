package frc.robot.lab;

import edu.wpi.first.math.geometry.Pose2d;

/**
 * The lab's stand-in for motor controller and sensor libraries: on a real robot, this is where
 * you'd call a Spark MAX, a Talon FX or a Pigeon.
 *
 * <p>Just like real hardware, none of this exists during log replay. In a replay, every read gives
 * zero or false, every write goes nowhere, and the lab notes which line of your code tried, so it
 * can tell you. Only an IO implementation should ever use this class.
 *
 * <p>You don't need to edit this file.
 */
public final class Hardware {
  private static final World WORLD = new World();

  private static boolean replaying = false;
  private static int touches = 0;
  private static String firstTouch = "";

  private Hardware() {}

  static World world() {
    return WORLD;
  }

  static void enterReplay() {
    replaying = true;
  }

  static int touches() {
    return touches;
  }

  static String firstTouch() {
    return firstTouch;
  }

  /** Notes that robot code used hardware during a replay. True if it did. */
  private static boolean absent() {
    if (!replaying) {
      return false;
    }
    if (touches == 0) {
      firstTouch =
          StackWalker.getInstance()
              .walk(
                  frames ->
                      frames
                          .filter(frame -> frame.getClassName().startsWith("frc.robot."))
                          .filter(frame -> !frame.getClassName().startsWith("frc.robot.lab."))
                          .findFirst()
                          .map(frame -> frame.getFileName() + ", line " + frame.getLineNumber())
                          .orElse("the lab"));
    }
    touches++;
    return true;
  }

  /** The motor that spins the shooter's flywheel. */
  public static final class FlywheelMotor {
    private FlywheelMotor() {}

    public void setVoltage(double volts) {
      if (!absent()) {
        WORLD.setFlywheelVolts(volts);
      }
    }

    /** How fast the flywheel is spinning. */
    public double getVelocityRadPerSec() {
      return absent() ? 0 : WORLD.flywheelRadPerSec();
    }

    public double getAppliedVolts() {
      return absent() ? 0 : WORLD.flywheelVolts();
    }

    public double getCurrentAmps() {
      return absent() ? 0 : WORLD.flywheelAmps();
    }
  }

  /** The motor that pushes a game piece into the flywheel, and the beam break watching it. */
  public static final class FeederMotor {
    private FeederMotor() {}

    public void setVoltage(double volts) {
      if (!absent()) {
        WORLD.setFeederVolts(volts);
      }
    }

    public double getAppliedVolts() {
      return absent() ? 0 : WORLD.feederVolts();
    }

    public double getCurrentAmps() {
      return absent() ? 0 : WORLD.feederAmps();
    }

    /** The beam break: true while a game piece is in the feeder. */
    public boolean hasPiece() {
      return !absent() && WORLD.hasPiece();
    }
  }

  /** The drivetrain's motors and encoders, boiled down to speeds and a position. */
  public static final class Drivetrain {
    private Drivetrain() {}

    /** Field-relative speeds: meters per second, and radians per second counterclockwise. */
    public void setSpeeds(double vxMetersPerSec, double vyMetersPerSec, double omegaRadPerSec) {
      if (!absent()) {
        WORLD.setSpeeds(vxMetersPerSec, vyMetersPerSec, omegaRadPerSec);
      }
    }

    public Pose2d getPose() {
      return absent() ? Pose2d.kZero : WORLD.pose();
    }
  }

  /** A fast gyro: it takes five samples for every robot loop. */
  public static final class Gyro {
    private Gyro() {}

    /** The turn rate samples (rad/s) taken since the last loop, oldest first. */
    public double[] getYawRateSamples() {
      return absent() ? new double[0] : WORLD.gyroRates().clone();
    }

    /** When each of those samples was taken, in seconds. */
    public double[] getSampleTimestamps() {
      return absent() ? new double[0] : WORLD.gyroTimes().clone();
    }

    /** The gyro's own answer for the robot's heading, in radians. */
    public double getYawRad() {
      return absent() ? 0 : WORLD.yawRad();
    }
  }

  /** The motor and encoder of a pivoting arm that swings between 0 and about 126 degrees. */
  public static final class PivotMotor {
    private PivotMotor() {}

    public void setVoltage(double volts) {
      if (!absent()) {
        WORLD.setPivotVolts(volts);
      }
    }

    public double getPositionRad() {
      return absent() ? 0 : WORLD.pivotRad();
    }

    public double getVelocityRadPerSec() {
      return absent() ? 0 : WORLD.pivotRadPerSec();
    }

    public double getAppliedVolts() {
      return absent() ? 0 : WORLD.pivotVolts();
    }
  }

  public static final FlywheelMotor flywheel = new FlywheelMotor();
  public static final FeederMotor feeder = new FeederMotor();
  public static final Drivetrain drive = new Drivetrain();
  public static final Gyro gyro = new Gyro();
  public static final PivotMotor pivot = new PivotMotor();
}
