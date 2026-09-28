package frc.robot.subsystems.LEDS;

import edu.wpi.first.wpilibj.AddressableLED;
import edu.wpi.first.wpilibj.AddressableLEDBuffer;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.littletonrobotics.junction.Logger;

/**
 * Drives the robot's LED strip and runs the challenge the dashboard asks for.
 *
 * <p>The <b>LED Challenges</b> dashboard writes {@code LEDs/RunRequest} in the SmartDashboard
 * table, as {@code "<challenge id>#<a number that changes on every click>"}; an empty id means
 * stop. This class logs the strip's colors, the running challenge and any error under {@code LEDs/}
 * so the dashboard (and AdvantageScope) can show them.
 *
 * <p>You don't need to edit this file.
 */
public class LedSubsystem {
  /** The PWM port the LED strip is plugged into on a real robot. */
  public static final int PWM_PORT = 0;

  /** How many LEDs the strip has. */
  public static final int LED_COUNT = 30;

  private static final String RUN_REQUEST = "LEDs/RunRequest";

  private final AddressableLED hardware = new AddressableLED(PWM_PORT);
  private final AddressableLEDBuffer buffer = new AddressableLEDBuffer(LED_COUNT);
  private final LedStrip strip = new LedStrip(buffer);
  private final String[] challengeIds = LedChallenges.ids();

  private String lastRequest = "";
  private String runningId = "";
  private Led challenge = null;
  private double startTime = 0.0;
  private double seconds = 0.0;
  private String error = "";

  public LedSubsystem() {
    hardware.setLength(LED_COUNT);
    hardware.setData(buffer);
    hardware.start();
    SmartDashboard.setDefaultString(RUN_REQUEST, "");
  }

  /** Called every loop (about every 20 ms), enabled or not. */
  public void periodic() {
    String request = SmartDashboard.getString(RUN_REQUEST, "");
    if (!request.equals(lastRequest)) {
      lastRequest = request;
      int hash = request.lastIndexOf('#');
      run(hash >= 0 ? request.substring(0, hash) : request);
    }

    if (challenge != null) {
      seconds = Timer.getFPGATimestamp() - startTime;
      try {
        challenge.update(strip, seconds);
      } catch (RuntimeException | StackOverflowError e) {
        fail("update()", e);
      }
    }

    hardware.setData(buffer);

    Logger.recordOutput("LEDs/Colors", strip.toPackedArray());
    Logger.recordOutput("LEDs/Running", runningId);
    Logger.recordOutput("LEDs/Seconds", seconds);
    Logger.recordOutput("LEDs/Error", error);
    Logger.recordOutput("LEDs/Challenges", challengeIds);
  }

  /** Starts the challenge with this id, or stops the current one if the id is empty. */
  private void run(String id) {
    challenge = null;
    runningId = "";
    seconds = 0.0;
    error = "";
    strip.clear();
    if (id.isEmpty()) {
      return;
    }

    runningId = id;
    try {
      challenge = LedChallenges.create(id);
    } catch (RuntimeException | StackOverflowError e) {
      // A field initializer or constructor in the challenge threw.
      fail("its constructor", e);
      return;
    }
    if (challenge == null) {
      error = "The robot program has no challenge called \"" + id + "\".";
      runningId = "";
      return;
    }

    System.out.println("[LEDs] Running challenge: " + id);
    startTime = Timer.getFPGATimestamp();
    try {
      challenge.start(strip);
    } catch (RuntimeException | StackOverflowError e) {
      fail("start()", e);
    }
  }

  /** Stops the challenge after it throws, and reports where the problem was. */
  private void fail(String where, Throwable e) {
    String message = e.getClass().getSimpleName();
    if (e.getMessage() != null) {
      message += ": " + e.getMessage();
    }
    for (StackTraceElement frame : e.getStackTrace()) {
      if (frame.getClassName().startsWith(LedSubsystem.class.getPackageName() + ".Challenge")) {
        message += " (" + frame.getFileName() + ", line " + frame.getLineNumber() + ")";
        break;
      }
    }
    error = message;
    DriverStation.reportError(
        "[LEDs] Challenge \"" + runningId + "\" stopped in " + where + ": " + message,
        e.getStackTrace());
    challenge = null;
    runningId = "";
  }
}
