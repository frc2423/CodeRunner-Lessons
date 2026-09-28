package frc.robot.subsystems.LEDS;

/**
 * Example: Hello, LEDs. This one is already finished, so you can see how a challenge works.
 *
 * <p>Pick <b>Example: Hello, LEDs</b> on the LED Challenges dashboard and click <b>Run</b>. The
 * first three LEDs turn red, green and blue.
 */
public class Challenge00Example implements Led {
  @Override
  public void update(LedStrip leds, double seconds) {
    // setRGB(which LED, red, green, blue). Each color amount goes from 0 to 255.
    leds.setRGB(0, 255, 0, 0); // LED 0: red
    leds.setRGB(1, 0, 255, 0); // LED 1: green
    leds.setRGB(2, 0, 0, 255); // LED 2: blue
  }
}
