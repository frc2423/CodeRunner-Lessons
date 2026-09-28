package frc.robot.subsystems.LEDS;

import edu.wpi.first.wpilibj.util.Color;

/**
 * Challenge 12: Color Palette (arrays).
 *
 * <ol>
 *   <li>Finish the array of 4 colors: your team's palette.
 *   <li>Paint the strip with the palette repeating: LED 0 gets color 0, LED 1 color 1, ..., LED 4
 *       color 0 again.
 *   <li>Every quarter of a second, shift the pattern one LED along the strip.
 * </ol>
 */
public class Challenge12Palette implements Led {
  /** The colors to repeat along the strip. */
  private final Color[] palette = {
    Color.kRed,
    // TODO: Add three more colors, such as Color.kOrange. Put a comma between them.
  };

  @Override
  public void update(LedStrip leds, double seconds) {
    // TODO: Loop over every LED and pick its color from the array.
    // Use palette.length, not 4, so you can add more colors later.
  }
}
