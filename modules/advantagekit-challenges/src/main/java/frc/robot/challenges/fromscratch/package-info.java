/**
 * Your pivot goes here: {@code PivotIO.java}, {@code PivotIOSim.java} and {@code Pivot.java}.
 *
 * <p>The hardware is {@code Hardware.pivot}: {@code setVoltage(volts)}, {@code getPositionRad()},
 * {@code getVelocityRadPerSec()} and {@code getAppliedVolts()}. The arm swings between 0 and about
 * 126 degrees, and at 12 volts it moves at about 2.5 radians per second.
 *
 * <p>A simple controller works well: {@code volts = kP * (setpoint - position)} with {@code kP}
 * around 12 volts per radian, clamped to 12 volts. WPILib's {@code PIDController} works too.
 */
package frc.robot.challenges.fromscratch;
