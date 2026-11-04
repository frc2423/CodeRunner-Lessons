package frc.robot.challenges.fastsensors;

import org.littletonrobotics.junction.AutoLog;

/**
 * The gyro's IO layer.
 *
 * <p>On a real robot, a background thread reads a fast sensor and queues up the samples. Threads
 * can't be replayed, so the thread stays inside the IO implementation, and it hands over everything
 * it collected once per loop as an input. The rest of the code stays on one thread.
 */
public interface GyroIO {
  @AutoLog
  public static class GyroIOInputs {
    // Arrays need a starting value too. A null input would crash the logger.
    public double[] yawRateRadPerSec = new double[] {};
    public double[] sampleTimestamps = new double[] {};
  }

  /** Updates the set of loggable inputs. */
  public default void updateInputs(GyroIOInputs inputs) {}
}
