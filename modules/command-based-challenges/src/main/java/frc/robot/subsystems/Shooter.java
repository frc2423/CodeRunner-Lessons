package frc.robot.subsystems;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.Hardware;
import org.littletonrobotics.junction.Logger;

/**
 * The shooter: a flywheel that launches game pieces at the hub. Its motor controller holds whatever
 * speed you ask for, but the wheel takes about a second to spin up.
 *
 * <p>A piece flies about 1 meter for every 1000 RPM. Feed it in before the wheel is up to speed and
 * it falls short, or (under 1500 RPM) just dribbles out.
 */
public class Shooter extends SubsystemBase {
  /**
   * The speed that scores from {@link frc.robot.FieldConstants#SHOOTING_SPOT}, 3 m from the hub.
   */
  public static final double SHOT_RPM = 3000;

  /** How close to the target speed counts as "at speed". */
  public static final double TOLERANCE_RPM = 150;

  @Override
  public void periodic() {
    Logger.recordOutput("Shooter/RPM", getRPM());
    Logger.recordOutput("Shooter/TargetRPM", getTargetRPM());
    Logger.recordOutput("Shooter/AtSpeed", atSpeed());
  }

  /** Asks the flywheel to spin at this speed and keep spinning, until you change it. */
  public void setRPM(double rpm) {
    Hardware.setShooterTargetRpm(rpm);
  }

  /** Lets the flywheel coast to a stop. */
  public void stop() {
    Hardware.setShooterTargetRpm(0);
  }

  /** How fast the flywheel is really spinning. */
  public double getRPM() {
    return Hardware.shooterRpm();
  }

  /** The speed the flywheel was last asked for. */
  public double getTargetRPM() {
    return Hardware.shooterTargetRpm();
  }

  /** True when the flywheel has been asked to spin, and is within 150 RPM of that speed. */
  public boolean atSpeed() {
    double target = getTargetRPM();
    return target > 0 && Math.abs(getRPM() - target) <= TOLERANCE_RPM;
  }

  /** The speed that lands a piece {@code meters} away from the robot's center. */
  public static double rpmForDistance(double meters) {
    return (meters - 0.2) * 1000.0;
  }
}
