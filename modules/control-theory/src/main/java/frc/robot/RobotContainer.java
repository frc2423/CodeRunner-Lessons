// Copyright (c) 2021-2026 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package frc.robot;

import frc.robot.lab.ControlLab;

/**
 * Holds the robot's subsystems. {@link Robot} handles the AdvantageKit logging setup and calls
 * {@link #robotPeriodic()} every loop.
 *
 * <p>This lesson has one subsystem: the control lab, a test bench of simulated mechanisms. The
 * controllers you write live in {@code challenges/}, one class per challenge. You should not need
 * to edit this file.
 */
public class RobotContainer {
  private final ControlLab lab = new ControlLab();

  /** Called every loop (about every 20 ms), enabled or not. */
  public void robotPeriodic() {
    lab.periodic();
  }
}
