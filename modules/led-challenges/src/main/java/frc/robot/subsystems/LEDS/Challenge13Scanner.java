package frc.robot.subsystems.LEDS;

/**
 * Challenge 13: Scanner.
 *
 * <p>Make a red dot bounce back and forth between the two ends of the strip, like a robot's
 * scanning eye. Move it one LED every 2 updates.
 *
 * <p>This time the dot has to remember two things between updates: where it is, and which way it is
 * going.
 *
 * <p>Bonus: leave a fading tail behind the dot.
 */
public class Challenge13Scanner implements Led {
  // TODO: Add fields for the dot's position and direction (+1 or -1), and an update counter.

  @Override
  public void update(LedStrip leds, double seconds) {
    // TODO: Draw the dot.

    // TODO: Every 2 updates, move the dot. When it reaches either end, turn it around.
  }
}
