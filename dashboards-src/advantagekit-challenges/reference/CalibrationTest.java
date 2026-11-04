package frc.robot.lab;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.first.hal.AllianceStationID;
import edu.wpi.first.hal.HAL;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StringSubscriber;
import edu.wpi.first.wpilibj.simulation.DriverStationSim;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Robot;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Drives the real robot program the way the dashboard does: asks for a trial over NetworkTables,
 * enables the robot, and reads the graded result, with real replays in real child JVMs.
 *
 * <p>Set the environment variable {@code AKITLAB_CAL} to:
 *
 * <ul>
 *   <li>{@code record}: on the unmodified scaffolds, record the shipped logs into src/main/deploy.
 *   <li>{@code scaffold}: the unmodified scaffolds should pass nothing but the example.
 *   <li>{@code solved}: with reference/solutions copied over them, everything should pass, stars
 *       included.
 *   <li>{@code naive}: with reference/naive copied over the solutions, each attempt makes the mistake
 *       its challenge teaches, so each of them should fail.
 * </ul>
 *
 * <p>Set {@code AKITLAB_CAL_ONLY} to a comma-separated list of ids to run just those. See ../README.md
 * for how to run it. Kept out of the module so students don't see it.
 */
class CalibrationTest {
  /** The scaffolds that should pass anyway. */
  private static final List<String> PASSES_AS_SCAFFOLD = List.of("example");

  /** The challenges with a deliberately naive attempt in reference/naive. */
  private static final List<String> NAIVE =
      List.of(
          "first-output",
          "every-loop",
          "process-inputs",
          "no-peeking",
          "swap-io",
          "hindsight",
          "deterministic",
          "dashboard-inputs",
          "log-commands",
          "fast-sensors",
          "from-scratch",
          "mystery");

  /** Most challenges have every star available to a good solution. */
  private static final Map<String, Integer> STARS =
      Map.of(
          "first-output", 1,
          "poses", 1,
          "hindsight", 1,
          "dashboard-inputs", 1,
          "from-scratch", 1,
          "mystery", 1);

  private static final ObjectMapper JSON = new ObjectMapper();

  private StringSubscriber result;
  private StringSubscriber status;

  @Test
  void calibrate() throws Exception {
    String mode = System.getenv().getOrDefault("AKITLAB_CAL", "solved");
    startRobot();
    Thread.sleep(1500);

    List<String> failures = new ArrayList<>();
    if (mode.equals("record")) {
      record("record-practice-match", Challenges.PRACTICE_MATCH, failures);
      record("record-qual-17", Challenges.QUAL_17, failures);
    } else {
      String only = System.getenv("AKITLAB_CAL_ONLY");
      List<String> ids =
          mode.equals("naive")
              ? NAIVE
              : only == null ? Challenges.ids() : List.of(only.split(","));
      for (String id : ids) {
        JsonNode graded = run(id);
        print(id, graded);
        boolean passed = graded != null && graded.get("passed").asBoolean();
        int stars = graded == null ? 0 : graded.get("stars").asInt();
        if (mode.equals("naive")) {
          if (passed) {
            failures.add(id + " passes with a naive attempt");
          }
        } else if (mode.equals("solved")) {
          if (!passed) {
            failures.add(id + " should pass");
          }
          if (stars != STARS.getOrDefault(id, 0)) {
            failures.add(id + " earned " + stars + " stars");
          }
        } else if (passed != PASSES_AS_SCAFFOLD.contains(id)) {
          failures.add(id + (passed ? " passes before the student did anything" : " should pass"));
        }
      }
    }
    System.out.println(failures.isEmpty() ? "ALL OK" : "PROBLEMS: " + failures);
    assertTrue(failures.isEmpty(), failures.toString());
  }

  private void startRobot() {
    HAL.initialize(500, 0);
    DriverStationSim.setDsAttached(true);
    DriverStationSim.setAllianceStationId(AllianceStationID.Blue1);
    DriverStationSim.setAutonomous(false);
    DriverStationSim.setTest(false);
    DriverStationSim.setEnabled(false);
    DriverStationSim.notifyNewData();

    NetworkTableInstance nt = NetworkTableInstance.getDefault();
    result = nt.getStringTopic("/AKitLab/Result").subscribe("");
    status = nt.getStringTopic("/AKitLab/Status").subscribe("");

    Thread robot =
        new Thread(
            () -> {
              Robot program = new Robot();
              program.startCompetition();
            },
            "Robot");
    robot.setDaemon(true);
    robot.start();
  }

  private void setEnabled(boolean enabled) {
    DriverStationSim.setEnabled(enabled);
    DriverStationSim.notifyNewData();
  }

  /** Asks for a trial, enables the robot if it needs that, and waits for the graded result. */
  private JsonNode run(String id) throws Exception {
    setEnabled(false);
    Thread.sleep(150);
    String before = result.get();
    SmartDashboard.putString("AKitLab/RunRequest", id + "|trial#" + System.nanoTime());
    Thread.sleep(200);
    setEnabled(true);

    long deadline = System.currentTimeMillis() + 90_000;
    while (System.currentTimeMillis() < deadline) {
      String latest = result.get();
      if (!latest.equals(before) && !latest.isEmpty()) {
        setEnabled(false);
        return JSON.readTree(latest);
      }
      JsonNode now = status.get().isEmpty() ? null : JSON.readTree(status.get());
      if (now != null && !now.get("error").asText().isEmpty()) {
        System.out.println("    ERROR: " + now.get("error").asText());
        setEnabled(false);
        return null;
      }
      Thread.sleep(50);
    }
    setEnabled(false);
    System.out.println("    TIMED OUT, status: " + status.get());
    return null;
  }

  private static void print(String id, JsonNode graded) {
    if (graded == null) {
      System.out.println("NOPE " + id + ": no result");
      return;
    }
    System.out.println(
        (graded.get("passed").asBoolean() ? "OK   " : "NOPE ")
            + id
            + " ("
            + graded.get("stars").asInt()
            + " stars)");
    for (JsonNode check : graded.get("checks")) {
      System.out.println(
          "    "
              + (check.get("pass").asBoolean() ? "pass " : "FAIL ")
              + (check.get("bonus").asBoolean() ? "[bonus] " : "")
              + check.get("label").asText()
              + " -- "
              + check.get("detail").asText());
    }
    if (graded.has("replayError")) {
      System.out.println("    replay error: " + graded.get("replayError").asText());
    }
  }

  /** Runs a recording trial and saves its log for the module to ship. */
  private void record(String id, String file, List<String> failures) throws Exception {
    JsonNode graded = run(id);
    print(id, graded);
    if (graded == null || !graded.get("passed").asBoolean()) {
      failures.add(id + " didn't replay cleanly");
      return;
    }
    Path target = Path.of("src", "main", "deploy", "match-logs", file);
    Files.createDirectories(target.getParent());
    Files.copy(Path.of("logs", "last-run.wpilog"), target, StandardCopyOption.REPLACE_EXISTING);
    System.out.println("    wrote " + target + " (" + Files.size(target) + " bytes)");
  }
}
