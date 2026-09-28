package frc.robot.subsystems.LEDS;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The list of challenges the dashboard can run. Each one has an id (the dashboard uses it to ask
 * for a challenge) and the class that implements it.
 *
 * <p>You don't need to edit this file. To run a light show of your own from the dashboard, write it
 * in a class that implements {@link Led} and point the {@code "freestyle"} line at it.
 */
public final class LedChallenges {
  private static final Map<String, Supplier<Led>> CHALLENGES = new LinkedHashMap<>();

  static {
    CHALLENGES.put("example", Challenge00Example::new);
    CHALLENGES.put("first-light", Challenge01FirstLight::new);
    CHALLENGES.put("mixing-colors", Challenge02MixingColors::new);
    CHALLENGES.put("variables", Challenge03Variables::new);
    CHALLENGES.put("fill-strip", Challenge04FillStrip::new);
    CHALLENGES.put("stripes", Challenge05Stripes::new);
    CHALLENGES.put("gradient", Challenge06Gradient::new);
    CHALLENGES.put("blink", Challenge07Blink::new);
    CHALLENGES.put("moving-dot", Challenge08MovingDot::new);
    CHALLENGES.put("progress-bar", Challenge09ProgressBar::new);
    CHALLENGES.put("flag", Challenge10Flag::new);
    CHALLENGES.put("rainbow", Challenge11Rainbow::new);
    CHALLENGES.put("palette", Challenge12Palette::new);
    CHALLENGES.put("scanner", Challenge13Scanner::new);
    CHALLENGES.put("twinkle", Challenge14Twinkle::new);
    CHALLENGES.put("morse-code", Challenge15MorseCode::new);
    CHALLENGES.put("automaton", Challenge16Automaton::new);
    CHALLENGES.put("robot-status", Challenge17RobotStatus::new);
    CHALLENGES.put("freestyle", Challenge18Freestyle::new);
  }

  private LedChallenges() {}

  /** Every challenge id, in order. */
  public static String[] ids() {
    return CHALLENGES.keySet().toArray(new String[0]);
  }

  /** A new object of the challenge with this id, or null if there is no such challenge. */
  public static Led create(String id) {
    Supplier<Led> factory = CHALLENGES.get(id);
    return factory == null ? null : factory.get();
  }
}
