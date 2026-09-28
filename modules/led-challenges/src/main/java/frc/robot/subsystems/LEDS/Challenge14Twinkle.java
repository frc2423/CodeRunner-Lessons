package frc.robot.subsystems.LEDS;

import java.util.Random;

/**
 * Challenge 14: Twinkle.
 *
 * <p>Make the strip twinkle like stars: every update, each LED
 *
 * <ol>
 *   <li>fades a little (its red, green and blue are each multiplied by 0.9), and
 *   <li>has a small chance (2%) of lighting up bright white.
 * </ol>
 */
public class Challenge14Twinkle implements Led {
  private final Random random = new Random();

  @Override
  public void update(LedStrip leds, double seconds) {
    // TODO: Loop over every LED.
    //   - Read its current color with leds.getRed(i), leds.getGreen(i) and leds.getBlue(i).
    //   - Make it a bit darker.
    //   - Roll a random number to decide whether it sparkles instead.
  }
}
