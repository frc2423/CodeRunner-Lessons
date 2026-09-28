package frc.robot.subsystems.LEDS;

/**
 * Challenge 8: Moving Dot.
 *
 * <p>Make one green dot travel along the strip. When it goes past the last LED, it starts again at
 * LED 0.
 *
 * <ol>
 *   <li>Move the dot one LED every update. (It will be fast!)
 *   <li>Slow it down: count updates, and only move the dot every 5th update.
 * </ol>
 *
 * <p>The dot has to remember where it is between updates, so its position must be a <b>field</b> (a
 * variable that belongs to the object), not a variable inside {@code update}.
 */
public class Challenge08MovingDot implements Led {
  // TODO: Add a field to remember the dot's position.

  @Override
  public void update(LedStrip leds, double seconds) {
    // TODO: Turn every LED off, light the dot, then move the dot forward one LED.
    // Don't let it run off the end of the strip!
  }
}
