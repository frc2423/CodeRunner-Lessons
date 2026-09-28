package frc.robot.subsystems.LEDS;

/**
 * Challenge 6: Gradient.
 *
 * <p>Fade smoothly from pure red at the first LED to pure blue at the last LED. Each LED's color is
 * worked out from its index with math.
 */
public class Challenge06Gradient implements Led {
  @Override
  public void update(LedStrip leds, double seconds) {
    // TODO: Loop over every LED.
    //   1. Work out how far along the strip the LED is, from 0.0 (first) to 1.0 (last).
    //   2. Blue goes up with that amount, red goes down.
    //   3. Convert to whole numbers from 0 to 255 and set the LED.
  }
}
