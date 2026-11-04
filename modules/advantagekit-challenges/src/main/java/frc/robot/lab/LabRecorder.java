package frc.robot.lab;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.littletonrobotics.junction.LogDataReceiver;
import org.littletonrobotics.junction.LogTable;
import org.littletonrobotics.junction.wpilog.WPILOGWriter;

/**
 * Saves one graded trial to its own log file, the same way a robot saves a match to a USB stick.
 * AdvantageKit hands this class a copy of the whole log table after every loop, on its own thread.
 *
 * <p>You don't need to edit this file.
 */
public final class LabRecorder implements LogDataReceiver {
  private static final LabRecorder INSTANCE = new LabRecorder();

  public static LabRecorder get() {
    return INSTANCE;
  }

  /** One recording: the file, and the robot times (in microseconds) it covers. */
  private static final class Recording {
    final String path;
    final long startTime;
    final CountDownLatch finished = new CountDownLatch(1);
    volatile long endTime = Long.MAX_VALUE;

    Recording(String path, long startTime) {
      this.path = path;
      this.startTime = startTime;
    }
  }

  private volatile Recording recording = null;

  // Used only on AdvantageKit's receiver thread.
  private Recording writing = null;
  private WPILOGWriter writer = null;
  private LogTable previous = null;

  private LabRecorder() {}

  /** Starts saving every loop from the robot time {@code start} (in microseconds) on. */
  void begin(String file, long start) {
    new File(file).getParentFile().mkdirs();
    new File(file).delete();
    recording = new Recording(file, start);
  }

  /** Stops after the loop at the robot time {@code end} (in microseconds). */
  void end(long end) {
    Recording current = recording;
    if (current != null) {
      current.endTime = end;
    }
  }

  /** Stops right away and forgets the recording. */
  void cancel() {
    recording = null;
  }

  /** Waits for the file to be closed. False if that's taking too long. */
  boolean awaitFinished(long seconds) {
    Recording current = recording;
    if (current == null) {
      return true;
    }
    try {
      return current.finished.await(seconds, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  @Override
  public void putTable(LogTable table) {
    write(table);
    previous = table;
  }

  private void write(LogTable table) {
    Recording current = recording;
    // A cancelled or replaced recording: close its file.
    if (writing != null && writing != current) {
      close();
    }
    if (current == null || current.finished.getCount() == 0) {
      return;
    }
    long time = table.getTimestamp();
    if (time < current.startTime) {
      return;
    }
    if (time > current.endTime) {
      writing = current;
      close();
      return;
    }
    if (writer == null) {
      writing = current;
      writer = new WPILOGWriter(current.path, WPILOGWriter.AdvantageScopeOpenBehavior.NEVER);
      writer.start();
      writeInitCycle();
    }
    writer.putTable(table);
  }

  /**
   * AdvantageKit treats the first cycle of a log as the one where the robot's constructor ran, so a
   * replay doesn't call {@code robotPeriodic} for it. The trial's first loop needs that call, so
   * the loop before it goes first, with the last challenge's values taken out.
   */
  private void writeInitCycle() {
    if (previous == null) {
      return;
    }
    LogTable init = LogTable.clone(previous);
    LogAccess.removeStudentValues(init);
    writer.putTable(init);
  }

  private void close() {
    if (writer != null) {
      writer.end();
      writer = null;
    }
    if (writing != null) {
      writing.finished.countDown();
      writing = null;
    }
  }

  @Override
  public void end() {
    close();
  }
}
