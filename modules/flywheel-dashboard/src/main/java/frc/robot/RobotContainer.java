// Copyright (c) 2021-2026 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package frc.robot;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.littletonrobotics.junction.Logger;

/**
 * The flywheel-dashboard lesson lives here. {@link Robot} handles the AdvantageKit logging setup
 * and calls {@link #robotPeriodic()} every loop.
 *
 * <p>The <b>Flywheel Tuning</b> dashboard tab sends the target speed and the controller gains to
 * the robot through NetworkTables (the SmartDashboard table), and shows the values this class logs.
 */
public class RobotContainer {
  /** Free speed of the simulated flywheel at 12 volts. */
  private static final double MAX_RPM = 6000.0;

  /** How quickly the simulated flywheel responds, in seconds. */
  private static final double TIME_CONSTANT = 0.4;

  /** Robot loop period, in seconds. */
  private static final double LOOP_PERIOD = 0.02;

  private double velocityRpm = 0.0;

  public RobotContainer() {
    // Publish starting values so the dashboard's controls begin here.
    SmartDashboard.setDefaultNumber("Flywheel/TargetRPM", 3000.0);
    SmartDashboard.setDefaultNumber("Flywheel/kP", 0.002);
    SmartDashboard.setDefaultNumber("Flywheel/kV", 0.0);
  }

  /** Called every loop (about every 20 ms), enabled or not. */
  public void robotPeriodic() {
    // Values the dashboard's sliders write.
    double targetRpm = SmartDashboard.getNumber("Flywheel/TargetRPM", 3000.0);
    double kP = SmartDashboard.getNumber("Flywheel/kP", 0.002);
    double kV = SmartDashboard.getNumber("Flywheel/kV", 0.0);

    double volts = 0.0;
    if (DriverStation.isEnabled()) {
      double error = targetRpm - velocityRpm;
      volts = kP * error;
      // Lesson (step 3): add a feedforward term, kV * targetRpm, to the voltage.
    }
    volts = MathUtil.clamp(volts, -12.0, 12.0);

    simulateFlywheel(volts);

    Logger.recordOutput("Flywheel/TargetRPM", targetRpm);
    Logger.recordOutput("Flywheel/VelocityRPM", velocityRpm);
    Logger.recordOutput("Flywheel/Volts", volts);
    // Lesson (step 4): log "Flywheel/AtSpeed" as true when the flywheel is within 100 RPM of
    // the target. The dashboard shows it as soon as you publish it.
  }

  /** A simple flywheel model: the speed moves toward what the applied voltage can sustain. */
  private void simulateFlywheel(double volts) {
    double steadyStateRpm = volts / 12.0 * MAX_RPM;
    velocityRpm += (steadyStateRpm - velocityRpm) * LOOP_PERIOD / TIME_CONSTANT;
  }
}
