package frc.robot.lab;

import org.littletonrobotics.junction.AutoLog;

/**
 * What the driver's controller said this loop. The lab logs this as an input under {@code
 * Lab/Driver}, so a replay presses the same buttons at the same moments.
 */
@AutoLog
public class DriverInputs {
  /** Every button's state as bits, button 1 (A) in bit 0. */
  public long buttons = 0;

  /** The six axes, in {@code XboxController.Axis} order. */
  public double[] axes = new double[6];

  /** The d-pad angle in degrees, or -1 when it isn't pressed. */
  public int pov = -1;
}
