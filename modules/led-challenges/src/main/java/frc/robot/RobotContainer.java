// Copyright (c) 2021-2026 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package frc.robot;

import frc.robot.subsystems.LEDS.LedSubsystem;

/**
 * Holds the robot's subsystems. {@link Robot} handles the AdvantageKit logging setup and calls
 * {@link #robotPeriodic()} every loop.
 *
 * <p>This lesson has one subsystem: the LED strip. The challenges you write live in {@code
 * subsystems/LEDS/}, one class per challenge. You should not need to edit this file.
 */
public class RobotContainer {
  private final LedSubsystem leds = new LedSubsystem();

  /** Called every loop (about every 20 ms), enabled or not. */
  public void robotPeriodic() {
    leds.periodic();
  }
}
