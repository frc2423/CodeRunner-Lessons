package frc.robot.subsystems.LEDS;

/**
 * Challenge 11: Rainbow.
 *
 * <ol>
 *   <li>Spread one whole rainbow across the strip: the hue goes from 0 at the first LED almost all
 *       the way round to 360 at the last.
 *   <li>Make the rainbow slide along the strip by adding an amount that grows with {@code seconds}
 *       to every hue.
 * </ol>
 *
 * <p>Use {@code leds.setHSV(index, hue, saturation, value)}: it is much easier than mixing red,
 * green and blue for this.
 */
public class Challenge11Rainbow implements Led {
  @Override
  public void update(LedStrip leds, double seconds) {
    // TODO: Loop over every LED and give it a hue based on its index.

    // TODO: Then add a shift that grows over time to make the rainbow move.
  }
}
