package frc.robot.subsystems.LEDS;

/**
 * Challenge 17: Robot Status.
 *
 * <p>Real robots use LEDs to show the drive team what the robot is doing. Make the strip show the
 * robot's state from the Driver Station:
 *
 * <ul>
 *   <li>Disabled: slowly "breathe" (fade up and down, about once every 2 seconds) in the alliance
 *       color: red for the red alliance, blue for the blue alliance.
 *   <li>Enabled in autonomous: solid yellow.
 *   <li>Enabled in teleop: solid green.
 * </ul>
 *
 * <p>Test it by changing the Driver Station's mode, alliance and Enable/Disable while this
 * challenge runs.
 */
public class Challenge17RobotStatus implements Led {
  @Override
  public void update(LedStrip leds, double seconds) {
    // TODO: Ask the DriverStation class what state the robot is in.

    // TODO: Pick a color (and a brightness, when disabled) and fill the strip with it.
  }
}
