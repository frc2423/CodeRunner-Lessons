package frc.robot.lab;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A number you can change from the dashboard while the robot runs, such as a controller gain. Each
 * one gets a slider on the <b>Control Lab</b> dashboard, so you can tune without restarting.
 *
 * <pre>{@code
 * private final TunableNumber kP = new TunableNumber("kP", 0.0);
 *
 * public double calculate(Mechanism mechanism) {
 *   return kP.get() * error;
 * }
 * }</pre>
 *
 * <p>Make tunable numbers as fields of your challenge class. The dashboard remembers the values you
 * set, even after a restart. When you're happy with a value, copy it into your code as the default.
 *
 * <p>This is the same idea as the "LoggedTunableNumber" many FRC teams use. You don't need to edit
 * this file.
 */
public class TunableNumber {
  private static final String TABLE = "ControlLab/Tune/";

  /** The challenge whose controller is being built, so its numbers are kept apart from others. */
  private static String owner = "default";

  /** Every tunable number made so far, by challenge id, in order. */
  private static final Map<String, Map<String, Double>> registry = new LinkedHashMap<>();

  private final String key;
  private final double defaultValue;

  /**
   * @param name the slider's name on the dashboard, like "kP"
   * @param defaultValue the value to use until you change it on the dashboard
   */
  public TunableNumber(String name, double defaultValue) {
    this.key = TABLE + owner + "/" + name;
    this.defaultValue = defaultValue;
    registry.computeIfAbsent(owner, id -> new LinkedHashMap<>()).put(name, defaultValue);
    SmartDashboard.setDefaultNumber(key, defaultValue);
  }

  /** The current value: the dashboard's slider, or the default. */
  public double get() {
    return SmartDashboard.getNumber(key, defaultValue);
  }

  // Used by the lab ------------------------------------------------------------------------------

  /** Makes tunable numbers created from now on belong to this challenge. */
  static void setOwner(String challengeId) {
    owner = challengeId;
  }

  /** The names and default values of this challenge's tunable numbers. */
  static List<Map.Entry<String, Double>> registered(String challengeId) {
    return new ArrayList<>(registry.getOrDefault(challengeId, Map.of()).entrySet());
  }
}
