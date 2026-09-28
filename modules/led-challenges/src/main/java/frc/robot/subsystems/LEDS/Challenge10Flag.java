package frc.robot.subsystems.LEDS;

/**
 * Challenge 10: Flag (methods).
 *
 * <p>Split the strip into three equal sections and paint them red, white and blue, like a flag.
 *
 * <p>Instead of three loops that are almost the same, write <b>one method</b>, {@code fillRange},
 * that paints any range of LEDs any color. Then call it three times.
 */
public class Challenge10Flag implements Led {
  @Override
  public void update(LedStrip leds, double seconds) {
    // TODO: Work out how long each section is (a third of the strip).

    // TODO: Call fillRange three times: red, then white, then blue.
  }

  /**
   * Sets LEDs {@code start} up to (but not including) {@code end} to one color.
   *
   * <p>For example, {@code fillRange(leds, 0, 3, 255, 0, 0)} makes LEDs 0, 1 and 2 red.
   */
  private void fillRange(LedStrip leds, int start, int end, int red, int green, int blue) {
    // TODO: Write a loop that sets every LED from start to end - 1.
  }
}
