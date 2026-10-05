// Copyright (c) 2021-2026 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package frc.robot;

import frc.robot.lab.CommandLab;

/**
 * In a normal command-based robot, this class creates the subsystems, binds buttons to commands and
 * picks the autonomous command.
 *
 * <p>In this lesson, every challenge in {@code challenges/} is its own little RobotContainer, and
 * the command lab swaps between them when you pick one on the <b>Command Lab</b> dashboard. The lab
 * also runs the {@link edu.wpi.first.wpilibj2.command.CommandScheduler} every loop, just like the
 * line {@code CommandScheduler.getInstance().run()} in a normal robot. You should not need to edit
 * this file.
 */
public class RobotContainer {
  private final CommandLab lab = new CommandLab();

  /** Called every loop (about every 20 ms), in every mode. */
  public void robotPeriodic() {
    lab.periodic();
  }

  /** Called once when the robot is enabled in autonomous. */
  public void autonomousInit() {
    lab.autonomousInit();
  }

  /** Called once when the robot is enabled in teleop. */
  public void teleopInit() {
    lab.teleopInit();
  }
}
