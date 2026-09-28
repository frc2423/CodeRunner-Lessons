package frc.robot.subsystems.LEDS;

/**
 * Challenge 16: Cellular Automaton.
 *
 * <p>Each LED is a <b>cell</b> that is alive (lit) or dead (off). Start with only the middle cell
 * alive. Every 0.15 seconds, make a new generation with this rule:
 *
 * <p><b>A cell is alive in the next generation if exactly one of its two neighbors (left and right)
 * is alive now.</b>
 *
 * <p>The strip wraps around: the left neighbor of the first cell is the last cell, and the right
 * neighbor of the last cell is the first cell.
 *
 * <p>Watch the pattern it makes. It is called "Rule 90".
 */
public class Challenge16Automaton implements Led {
  /** Seconds between generations. */
  private static final double STEP = 0.15;

  // TODO: Add a boolean array for the cells, and a field to remember when the last step happened.

  @Override
  public void start(LedStrip leds) {
    // TODO: Make the cells array, one cell per LED, with only the middle cell alive.
  }

  @Override
  public void update(LedStrip leds, double seconds) {
    // TODO: When it is time for a new generation, work it out into a NEW array
    // (don't change the old one while you are still reading it), then use the new one.

    // TODO: Draw the cells: alive cells in a color of your choice, dead cells off.
  }
}
