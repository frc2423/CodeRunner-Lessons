package frc.robot.lab;

import edu.wpi.first.math.geometry.Pose2d;
import java.util.Map;
import org.littletonrobotics.junction.LogTable.LogValue;

/** One robot loop of a trial: everything the student's code had logged, and the true robot. */
record Frame(double time, Map<String, LogValue> log, World.State world) {
  /** An output's raw logged value, by its key under RealOutputs, or null if it isn't in the log. */
  LogValue output(String key) {
    return log.get("/RealOutputs/" + key);
  }

  /** An input's raw logged value, by its key at the top of the log, or null. */
  LogValue input(String key) {
    return log.get("/" + key);
  }

  /** An output as a number (true is 1), or null if it isn't in the log or isn't a number. */
  Double number(String key) {
    return LogAccess.number(output(key));
  }

  /** An input as a number (true is 1), or null if it isn't in the log or isn't a number. */
  Double inputNumber(String key) {
    return LogAccess.number(input(key));
  }

  Pose2d pose(String key) {
    return LogAccess.pose(output(key));
  }

  Pose2d[] poses(String key) {
    return LogAccess.poses(output(key));
  }

  /** An output as text, or null if it isn't logged as text. */
  String text(String key) {
    LogValue value = output(key);
    return value != null
            && value.type == org.littletonrobotics.junction.LogTable.LoggableType.String
        ? value.getString()
        : null;
  }
}
