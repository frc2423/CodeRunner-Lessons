package frc.robot.lab;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Runs a real AdvantageKit replay. It starts a second copy of this robot program with the {@code
 * AKIT_LOG_PATH} environment variable set, the same one AdvantageKit's {@code findReplayLog()}
 * looks for. That copy is in {@code Mode.REPLAY}: it reads the log with a {@code WPILOGReader},
 * runs your code as fast as it can with the logged inputs, writes a {@code _sim} log, and exits.
 *
 * <p>You don't need to edit this file.
 */
final class ReplayRunner {
  /** Environment variable that tells the replaying copy which challenge's code to run. */
  static final String CHALLENGE_VARIABLE = "AKITLAB_CHALLENGE";

  /** A replay stops if it takes longer than this: robot code is stuck in a loop. */
  static final long TIMEOUT_SECONDS = 45;

  /** How a replay went: the log it wrote, or what went wrong. */
  record Outcome(ReplayLog log, String error) {}

  private static final Path FOLDER = Path.of("build", "akitlab").toAbsolutePath();
  private static final Path SAVED_LOGS = Path.of("logs").toAbsolutePath();

  private volatile Outcome outcome = null;
  private volatile Process process = null;
  private volatile boolean abandoned = false;

  /** Where the next recorded trial's log goes. */
  static String trialLogPath() {
    return FOLDER.resolve("trial.wpilog").toString();
  }

  /** Replays {@code log} with the code of challenge {@code id}, on a background thread. */
  void start(Path log, String id, LabRecorder recorder) {
    Thread thread =
        new Thread(
            () -> {
              Outcome result;
              try {
                if (recorder != null && !recorder.awaitFinished(10)) {
                  throw new IOException("the trial's log file wasn't closed in time");
                }
                result = run(log, id);
              } catch (IOException | RuntimeException e) {
                result = new Outcome(null, "The replay couldn't run: " + e);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                result = new Outcome(null, "The replay was stopped.");
              }
              outcome = result;
            },
            "AKitLab replay");
    thread.setDaemon(true);
    thread.start();
  }

  boolean finished() {
    return outcome != null;
  }

  Outcome outcome() {
    return outcome;
  }

  /** Forgets this replay, and stops it if it's still going. */
  void abandon() {
    abandoned = true;
    Process running = process;
    if (running != null) {
      running.destroyForcibly();
    }
  }

  private Outcome run(Path log, String id) throws IOException, InterruptedException {
    // Each replay gets its own folder. An open log file can't be replaced on every platform.
    Path folder = FOLDER.resolve("replay-" + System.currentTimeMillis());
    Files.createDirectories(folder);
    deleteOldReplays(folder);
    Path input = folder.resolve("replay.wpilog");
    Path output = folder.resolve("replay_sim.wpilog");
    Files.copy(log, input, StandardCopyOption.REPLACE_EXISTING);

    Path console = folder.resolve("console.txt");
    ProcessBuilder builder = new ProcessBuilder(command());
    builder.directory(folder.toFile());
    builder.redirectErrorStream(true);
    builder.redirectOutput(console.toFile());
    Map<String, String> environment = builder.environment();
    // HALSim extensions would fight the robot program for its ports, and replay doesn't allow them.
    environment.remove("HALSIM_EXTENSIONS");
    environment.put("AKIT_LOG_PATH", input.toString());
    environment.put(CHALLENGE_VARIABLE, id);

    Process started = builder.start();
    process = started;
    if (abandoned) {
      started.destroyForcibly();
      return new Outcome(null, "The replay was stopped.");
    }
    if (!started.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
      started.destroyForcibly();
      return new Outcome(
          null,
          "The replay didn't finish in "
              + TIMEOUT_SECONDS
              + " seconds. Is there a loop that never ends in your code?");
    }
    if (abandoned) {
      return new Outcome(null, "The replay was stopped.");
    }
    if (!Files.exists(output)) {
      return new Outcome(
          null,
          "The replay stopped early (exit code " + started.exitValue() + "). " + tail(console));
    }

    saveForAdvantageScope(input, output);
    return new Outcome(ReplayLog.read(output.toString()), "");
  }

  /** Cleans up after earlier replays, as far as the files can be deleted yet. */
  private static void deleteOldReplays(Path keep) {
    File[] folders = FOLDER.toFile().listFiles((dir, name) -> name.startsWith("replay-"));
    if (folders == null) {
      return;
    }
    for (File folder : folders) {
      if (folder.toPath().equals(keep)) {
        continue;
      }
      File[] files = folder.listFiles();
      if (files != null) {
        for (File file : files) {
          file.delete();
        }
      }
      folder.delete();
    }
  }

  /** Keeps a copy of the last run where a team would look for logs. */
  private static void saveForAdvantageScope(Path input, Path output) {
    try {
      Files.createDirectories(SAVED_LOGS);
      Files.copy(input, SAVED_LOGS.resolve("last-run.wpilog"), StandardCopyOption.REPLACE_EXISTING);
      Files.copy(
          output, SAVED_LOGS.resolve("last-run_sim.wpilog"), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      System.out.println("[AKitLab] Couldn't save the logs: " + e);
    }
  }

  private static String tail(Path console) {
    try {
      List<String> lines = Files.readAllLines(console, StandardCharsets.UTF_8);
      StringBuilder out = new StringBuilder();
      for (int i = Math.max(0, lines.size() - 6); i < lines.size(); i++) {
        out.append(lines.get(i)).append('\n');
      }
      return out.toString().trim();
    } catch (IOException | RuntimeException e) {
      return "";
    }
  }

  private static List<String> command() {
    List<String> command = new ArrayList<>();
    String home = System.getProperty("java.home");
    command.add(
        ProcessHandle.current()
            .info()
            .command()
            .orElse(home + File.separator + "bin" + File.separator + "java"));
    for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
      // The same memory limits and native library path as this copy, but no debugger or test agent.
      if (!argument.startsWith("-agentlib") && !argument.startsWith("-javaagent")) {
        command.add(argument);
      }
    }
    command.add("-cp");
    command.add(absoluteClassPath());
    command.add("frc.robot.Main");
    return command;
  }

  /** The class path, usable from the replay's own working folder. */
  private static String absoluteClassPath() {
    List<String> parts = new ArrayList<>();
    for (String part : System.getProperty("java.class.path").split(File.pathSeparator)) {
      if (!part.isEmpty()) {
        parts.add(new File(part).getAbsolutePath());
      }
    }
    return String.join(File.pathSeparator, parts);
  }
}
