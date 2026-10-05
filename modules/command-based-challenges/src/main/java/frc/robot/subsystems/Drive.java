package frc.robot.subsystems;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.FieldConstants;
import frc.robot.lab.Hardware;
import org.littletonrobotics.junction.Logger;

/**
 * A swerve drivetrain: it can drive in any direction while turning. Speeds are in meters per second
 * and radians per second.
 *
 * <p>"Field-relative" means forward is away from your driver station (+X on the field), whichever
 * way the robot is facing. "Robot-relative" means forward is wherever the robot's front points.
 *
 * <p>Like every subsystem, a command that uses the drive should <b>require</b> it, so two commands
 * can't drive the robot at the same time. {@code drive.run(...)} and friends do that for you.
 */
public class Drive extends SubsystemBase {
  /** The robot's top speed, in meters per second. */
  public static final double MAX_SPEED = 4.0;

  /** The robot's top turning speed, in radians per second (one turn per second). */
  public static final double MAX_ANGULAR_SPEED = 2 * Math.PI;

  @Override
  public void periodic() {
    Logger.recordOutput("Drive/Pose", getPose());
    Logger.recordOutput("Drive/MeasuredSpeeds", getRobotRelativeSpeeds());
  }

  /**
   * Drives relative to the field: +X is away from your driver station, +Y is to the left, and a
   * positive rotation turns counterclockwise (left).
   */
  public void driveFieldRelative(double xSpeed, double ySpeed, double rotationSpeed) {
    runVelocity(
        ChassisSpeeds.fromFieldRelativeSpeeds(xSpeed, ySpeed, rotationSpeed, getRotation()));
  }

  /**
   * Drives relative to the robot: +X is toward the robot's front (where the intake is), +Y is to
   * the robot's left, and a positive rotation turns counterclockwise.
   */
  public void driveRobotRelative(double xSpeed, double ySpeed, double rotationSpeed) {
    runVelocity(new ChassisSpeeds(xSpeed, ySpeed, rotationSpeed));
  }

  /** Drives at these robot-relative speeds. PathPlanner uses this to follow paths. */
  public void runVelocity(ChassisSpeeds robotRelativeSpeeds) {
    Logger.recordOutput("Drive/RequestedSpeeds", robotRelativeSpeeds);
    Hardware.setDriveSpeeds(robotRelativeSpeeds);
  }

  /** Stops the robot. The motors keep doing what they were last told until you call this! */
  public void stop() {
    runVelocity(new ChassisSpeeds());
  }

  /** Where the robot thinks it is on the field (from its odometry). */
  public Pose2d getPose() {
    return Hardware.gyroAndOdometryPose();
  }

  /** Which way the robot is facing (from its gyro). 0 is facing away from your driver station. */
  public Rotation2d getRotation() {
    return getPose().getRotation();
  }

  /** Tells the odometry where the robot is. It doesn't move the robot! */
  public void resetPose(Pose2d pose) {
    Hardware.resetOdometry(pose);
  }

  /** How fast the robot is moving, relative to itself. */
  public ChassisSpeeds getRobotRelativeSpeeds() {
    return Hardware.measuredRobotRelativeSpeeds();
  }

  /** How far the center of the robot is from the center of the hub, in meters. */
  public double getDistanceToHub() {
    return getPose().getTranslation().getDistance(FieldConstants.HUB);
  }

  /** The direction the robot must face to point its shooter at the hub. */
  public Rotation2d getAngleToHub() {
    return FieldConstants.HUB.minus(getPose().getTranslation()).getAngle();
  }
}
