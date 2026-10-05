package frc.robot.subsystems;

import frc.robot.lab.Hardware;

/**
 * The climber's motor, its encoder and its bottom limit switch: the hardware your {@link Climber}
 * subsystem controls. On a real robot this would be a motor controller object like a TalonFX or
 * SparkMax.
 *
 * <p>The climber's hook slides up a tube from 0 m (all the way down) to {@link #MAX_HEIGHT} (a hard
 * stop at the top). At 12 V it moves 0.6 m per second. Driving into either end with power still on
 * strains the motor and the gearbox, so stop before you get there!
 */
public class ClimberMotor {
  /** The top of the climber's travel, where it hits the hard stop. */
  public static final double MAX_HEIGHT = 0.6;

  /** Sets the motor's voltage, from -12 to 12. Positive moves the hook up. */
  public void setVoltage(double volts) {
    Hardware.setClimberVolts(volts);
  }

  /** Turns the motor off. */
  public void stop() {
    Hardware.setClimberVolts(0);
  }

  /** How high the hook is, in meters, from the encoder. */
  public double getHeight() {
    return Hardware.climberHeight();
  }

  /** The limit switch at the bottom of the climber: true when the hook is all the way down. */
  public boolean isAtBottom() {
    return Hardware.climberBottomLimitSwitch();
  }
}
