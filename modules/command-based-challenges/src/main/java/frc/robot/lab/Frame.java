package frc.robot.lab;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj.util.Color;
import java.util.List;
import java.util.Map;

/** Everything about the robot at one moment of a trial, for grading. */
record Frame(
    double time,
    boolean enabled,
    int buttons,
    int pov,
    double[] axes,
    Pose2d pose,
    double speed,
    double omega,
    boolean deployRequested,
    double deploy,
    double rollers,
    double indexer,
    boolean hasPiece,
    boolean beamBreak,
    boolean jammed,
    double rpm,
    double targetRpm,
    boolean atSpeed,
    double climberHeight,
    double climberVolts,
    double hang,
    double strain,
    Color led,
    /** "red", "blue", or "" if the Driver Station hasn't said. */
    String alliance,
    int score,
    int misses,
    int dribbles,
    int pickups,
    double rumble,
    /** Subsystem name to the name of the command using it, for every busy subsystem. */
    Map<String, String> owners,
    /** Names of every command running. */
    List<String> commands) {

  boolean pressed(int button) {
    return (buttons & (1 << (button - 1))) != 0;
  }

  /** The command using this subsystem, or "" if none. */
  String owner(String subsystem) {
    return owners.getOrDefault(subsystem, "");
  }

  boolean intakeOut() {
    return deploy >= 0.9;
  }

  boolean intakeIn() {
    return deploy <= 0.1;
  }

  boolean intaking() {
    return deploy >= 0.9 && rollers >= 0.3;
  }

  /** The rough name of the LED color: off, red, orange, yellow, green, blue, purple or white. */
  String ledName() {
    return Leds.colorName(led);
  }

  private static final class Leds {
    static String colorName(Color c) {
      double r = c.red;
      double g = c.green;
      double b = c.blue;
      double max = Math.max(r, Math.max(g, b));
      double min = Math.min(r, Math.min(g, b));
      if (max < 0.15) {
        return "off";
      }
      if (max - min < 0.15 * max + 0.05) {
        return "white";
      }
      double hue;
      if (max == r) {
        hue = 60 * (((g - b) / (max - min)) % 6);
      } else if (max == g) {
        hue = 60 * ((b - r) / (max - min) + 2);
      } else {
        hue = 60 * ((r - g) / (max - min) + 4);
      }
      if (hue < 0) {
        hue += 360;
      }
      if (hue < 15 || hue >= 330) {
        return "red";
      }
      if (hue < 40) {
        return "orange";
      }
      if (hue < 70) {
        return "yellow";
      }
      if (hue < 170) {
        return "green";
      }
      if (hue < 260) {
        return "blue";
      }
      return "purple";
    }
  }

  static String colorName(Color color) {
    return Leds.colorName(color);
  }
}
