package frc.robot.lab;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.util.Color;

/**
 * The robot's simulated motors and sensors. The subsystems call these where a real robot would talk
 * to motor controllers, encoders and sensors over the CAN bus.
 *
 * <p>Use the subsystems instead of calling this class from your challenges. You don't need to edit
 * this file.
 */
public final class Hardware {
  private static final World world = new World();

  private Hardware() {}

  static World world() {
    return world;
  }

  // Drivetrain ---------------------------------------------------------------------------------

  public static void setDriveSpeeds(ChassisSpeeds robotRelative) {
    world.setDriveSpeeds(robotRelative);
  }

  public static Pose2d gyroAndOdometryPose() {
    return world.odometryPose();
  }

  public static void resetOdometry(Pose2d pose) {
    world.resetOdometry(pose);
  }

  public static ChassisSpeeds measuredRobotRelativeSpeeds() {
    return world.robotRelativeSpeeds();
  }

  // Intake ---------------------------------------------------------------------------------------

  public static void setIntakeDeployed(boolean deployed) {
    world.setIntakeDeployed(deployed);
  }

  /** 0 when stowed, 1 when all the way out. */
  public static double intakePosition() {
    return world.deploy();
  }

  public static void setIntakeRollers(double speed) {
    world.setRollers(speed);
  }

  public static double intakeRollers() {
    return world.rollers();
  }

  public static double intakeRollerCurrent() {
    return world.rollerCurrent();
  }

  // Indexer --------------------------------------------------------------------------------------

  public static void setIndexer(double speed) {
    world.setIndexer(speed);
  }

  public static double indexer() {
    return world.indexer();
  }

  public static boolean beamBreak() {
    return world.beamBreak();
  }

  // Shooter --------------------------------------------------------------------------------------

  public static void setShooterTargetRpm(double rpm) {
    world.setTargetRpm(rpm);
  }

  public static double shooterRpm() {
    return world.rpm();
  }

  public static double shooterTargetRpm() {
    return world.targetRpm();
  }

  // Climber --------------------------------------------------------------------------------------

  public static void setClimberVolts(double volts) {
    world.setClimberVolts(volts);
  }

  public static double climberHeight() {
    return world.climberHeight();
  }

  public static boolean climberBottomLimitSwitch() {
    return world.climberAtBottom();
  }

  // LEDs -----------------------------------------------------------------------------------------

  public static void setLeds(Color color) {
    world.setLed(color);
  }

  public static Color leds() {
    return world.led();
  }
}
