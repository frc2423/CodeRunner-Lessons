package frc.robot.subsystems.LEDS;

/**
 * Every LED challenge is a class that implements this interface.
 *
 * <p>When you pick a challenge on the <b>LED Challenges</b> dashboard and click <b>Run</b>, the
 * robot makes a new object of that challenge's class, calls {@link #start} once, and then calls
 * {@link #update} over and over, about 50 times a second, until you click <b>Stop</b> or run a
 * different challenge.
 *
 * <p>You don't need to edit this file.
 */
public interface Led {
  /**
   * Runs once when you click <b>Run</b>, before the first {@link #update}. Every LED starts off.
   *
   * <p>You don't have to write this method. Leave it out if your challenge doesn't need it.
   *
   * @param leds the LED strip to draw on
   */
  default void start(LedStrip leds) {}

  /**
   * Runs about every 20 milliseconds (50 times a second) while the challenge is running.
   *
   * <p>LEDs keep their color until you change them, so you only need to set the ones that change.
   *
   * @param leds the LED strip to draw on
   * @param seconds how many seconds have passed since you clicked <b>Run</b>
   */
  void update(LedStrip leds, double seconds);
}
