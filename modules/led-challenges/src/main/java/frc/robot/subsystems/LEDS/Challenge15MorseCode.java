package frc.robot.subsystems.LEDS;

/**
 * Challenge 15: Morse Code.
 *
 * <p>Blink a message in Morse code with the whole strip, then repeat it.
 *
 * <p>Morse timing is counted in <b>units</b>. Use 0.2 seconds for one unit:
 *
 * <ul>
 *   <li>dot: on for 1 unit
 *   <li>dash: on for 3 units
 *   <li>between the dots and dashes of one letter: off for 1 unit
 *   <li>between letters: off for 3 units
 *   <li>between words, and before the message repeats: off for 7 units
 * </ul>
 *
 * <p>Plan: in {@code start}, turn the message into a String of '1's (on) and '0's (off), one
 * character per unit. "E" (one dot) followed by the end-of-message pause is "10000000". Then in
 * {@code update}, work out which unit it is and look up that character.
 */
public class Challenge15MorseCode implements Led {
  /** The message to send. Letters and spaces only. */
  private static final String MESSAGE = "SOS";

  /** Seconds in one Morse unit. */
  private static final double UNIT = 0.2;

  /** Morse code for A to Z: MORSE[0] is A, MORSE[1] is B, and so on. */
  private static final String[] MORSE = {
    ".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....", "..", ".---", "-.-", ".-..", "--",
    "-.", "---", ".--.", "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-", "-.--", "--.."
  };

  // TODO: Add a field to hold the on/off pattern.

  @Override
  public void start(LedStrip leds) {
    // TODO: Build the pattern from MESSAGE, one letter at a time.
    // Hint: for a letter c, MORSE[c - 'A'] is its code.
  }

  @Override
  public void update(LedStrip leds, double seconds) {
    // TODO: Work out which unit of the pattern we are in (it repeats forever),
    // then turn the whole strip on or off.
  }
}
