package frc.robot.subsystems.LEDS;

/**
 * Challenge 9: Progress Bar.
 *
 * <p>Make a green bar that grows from empty to the whole strip over 3 seconds, then starts again
 * from empty. LEDs that are not part of the bar yet should glow dim gray (20, 20, 20).
 */
public class Challenge09ProgressBar implements Led {
  @Override
  public void update(LedStrip leds, double seconds) {
    // TODO: Work out how many seconds into the current 3-second cycle we are.

    // TODO: Turn that into how many LEDs should be lit.

    // TODO: Loop over every LED: green if it is part of the bar, dim gray if not.
  }
}
