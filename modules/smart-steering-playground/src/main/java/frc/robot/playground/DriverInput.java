package frc.robot.playground;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.XboxController;

/**
 * Turns the driver's sticks into the speeds they ask for. The sticks come from the gamepad (or
 * keyboard) on the CodeRunner Driver Station's port 0, or from the keyboard while the Playground
 * tab has focus.
 *
 * <ul>
 *   <li>Left stick: drive. Field-relative, so pushing it up always drives away from the blue driver
 *       station (to the right on the Playground tab), whichever way the robot faces.
 *   <li>Right stick, left and right: spin.
 * </ul>
 *
 * <p>You don't need to edit this file.
 */
final class DriverInput {
  static final int PORT = 0;
  static final double DEADBAND = 0.08;

  private double[] dashboardAxes = new double[6];
  private double leftX;
  private double leftY;
  private double rightX;

  /** The Playground tab's keyboard, as six axes in {@link XboxController.Axis} order. */
  void setDashboardAxes(double[] axes) {
    double[] clean = new double[6];
    for (int i = 0; axes != null && i < 6 && i < axes.length; i++) {
      clean[i] = Double.isFinite(axes[i]) ? MathUtil.clamp(axes[i], -1, 1) : 0;
    }
    dashboardAxes = clean;
  }

  /** Reads the sticks. Call once per loop, before {@link #requestedSpeeds()}. */
  void update() {
    leftX = axis(XboxController.Axis.kLeftX.value);
    leftY = axis(XboxController.Axis.kLeftY.value);
    rightX = axis(XboxController.Axis.kRightX.value);
  }

  /** The field-relative speeds the driver is asking for. */
  ChassisSpeeds requestedSpeeds() {
    // Stick up is negative Y on a gamepad, and should drive toward +X on the field.
    double x = -leftY;
    double y = -leftX;
    double magnitude = Math.min(1, Math.hypot(x, y));
    double scaled = shape(magnitude);
    double vx = magnitude > 1e-9 ? x / Math.hypot(x, y) * scaled : 0;
    double vy = magnitude > 1e-9 ? y / Math.hypot(x, y) * scaled : 0;
    double omega = Math.copySign(shape(Math.abs(rightX)), -rightX);
    return new ChassisSpeeds(
        vx * PlaygroundConstants.MAX_SPEED,
        vy * PlaygroundConstants.MAX_SPEED,
        omega * PlaygroundConstants.MAX_ANGULAR_SPEED);
  }

  /** The sticks as the robot sees them: left X, left Y, right X. */
  double[] sticks() {
    return new double[] {leftX, leftY, rightX};
  }

  /** Deadband, then square, so small pushes give fine control. */
  private static double shape(double value) {
    double banded = MathUtil.applyDeadband(value, DEADBAND);
    return banded * banded;
  }

  private double axis(int axis) {
    if (dashboardAxes[axis] != 0) {
      return dashboardAxes[axis];
    }
    if (DriverStation.getStickAxisCount(PORT) > axis) {
      return MathUtil.clamp(DriverStation.getStickAxis(PORT, axis), -1, 1);
    }
    return 0;
  }
}
