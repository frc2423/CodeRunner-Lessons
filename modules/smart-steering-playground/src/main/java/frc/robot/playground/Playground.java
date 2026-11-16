package frc.robot.playground;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rectangle2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StringPublisher;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Obstacles;
import frc.robot.SmartSteering;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.littletonrobotics.junction.Logger;

/**
 * The smart steering playground. Every loop it reads the driver's sticks, hands the speeds they ask
 * for to {@link SmartSteering#steer}, drives the simulated robot with whatever comes back, and
 * tells the <b>Playground</b> dashboard tab (and AdvantageScope) what happened.
 *
 * <p>The tab writes these SmartDashboard entries:
 *
 * <ul>
 *   <li>{@code Playground/Keys}: the tab's keyboard, as six controller axes;
 *   <li>{@code Playground/DrawnObstacles}: the rectangles drawn on the tab, five numbers each
 *       (center X, center Y, X width, Y width, rotation in radians);
 *   <li>{@code Playground/SmartSteering}: whether to call your steering code at all;
 *   <li>{@code Playground/Command}: {@code "reset#<a number that changes every click>"}.
 * </ul>
 *
 * <p>You don't need to edit this file.
 */
public class Playground {
  private static final String KEYS = "Playground/Keys";
  private static final String DRAWN = "Playground/DrawnObstacles";
  private static final String SMART = "Playground/SmartSteering";
  private static final String COMMAND = "Playground/Command";

  /** At most this many drawn obstacles are used. */
  static final int MAX_DRAWN = 100;

  private final World world = new World();
  private final DriverInput input = new DriverInput();
  private final SwerveDriveKinematics kinematics;
  private final StringPublisher configPublisher;

  private SmartSteering steering;
  private final List<Rectangle2d> walls = World.walls();
  private List<Rectangle2d> codeObstacles = List.of();
  private List<Rectangle2d> drawnObstacles = List.of();
  private List<Rectangle2d> allObstacles = List.of();
  private double[] lastDrawn = new double[0];
  private String lastCommand = "";

  private ChassisSpeeds requested = new ChassisSpeeds();
  private ChassisSpeeds steered = new ChassisSpeeds();
  private boolean smartOn = true;
  private double steerMicros = 0;
  private String steeringError = "";
  private String obstacleError = "";

  public Playground() {
    double halfL = PlaygroundConstants.ROBOT_LENGTH / 2 - 0.1;
    double halfW = PlaygroundConstants.ROBOT_WIDTH / 2 - 0.1;
    kinematics =
        new SwerveDriveKinematics(
            new Translation2d(halfL, halfW),
            new Translation2d(halfL, -halfW),
            new Translation2d(-halfL, halfW),
            new Translation2d(-halfL, -halfW));

    NetworkTableInstance nt = NetworkTableInstance.getDefault();
    configPublisher = nt.getStringTopic("/Playground/Config").publish();
    configPublisher.set(config());
    SmartDashboard.setDefaultNumberArray(KEYS, new double[6]);
    SmartDashboard.setDefaultNumberArray(DRAWN, new double[0]);
    SmartDashboard.setDefaultBoolean(SMART, true);
    SmartDashboard.setDefaultString(COMMAND, "");

    try {
      steering = new SmartSteering();
    } catch (RuntimeException | StackOverflowError | ExceptionInInitializerError e) {
      steering = null;
      steeringError = report("Creating SmartSteering", e);
    }

    try {
      List<Rectangle2d> fromCode = Obstacles.create();
      List<Rectangle2d> valid = new ArrayList<>();
      if (fromCode != null) {
        for (Rectangle2d obstacle : fromCode) {
          if (obstacle != null) {
            valid.add(obstacle);
          }
        }
      }
      codeObstacles = List.copyOf(valid);
    } catch (RuntimeException | StackOverflowError | ExceptionInInitializerError e) {
      obstacleError = report("Obstacles.create()", e);
    }
    rebuildObstacles();
  }

  /** Called every loop (about every 20 ms), in every mode. */
  public void periodic() {
    readDashboard();
    input.update();

    boolean driving = DriverStation.isTeleopEnabled();
    requested = driving ? input.requestedSpeeds() : new ChassisSpeeds();
    Pose2d pose = world.pose();
    ChassisSpeeds current = world.fieldSpeeds();

    Debug.clear();
    steered = requested;
    steerMicros = 0;
    if (smartOn && steering != null) {
      long start = System.nanoTime();
      try {
        ChassisSpeeds result = steering.steer(copy(requested), pose, copy(current), allObstacles);
        if (result == null) {
          steeringError = "SmartSteering.steer returned null. Return a ChassisSpeeds instead.";
        } else if (!Double.isFinite(result.vxMetersPerSecond)
            || !Double.isFinite(result.vyMetersPerSecond)
            || !Double.isFinite(result.omegaRadiansPerSecond)) {
          steeringError =
              "SmartSteering.steer returned "
                  + result
                  + ". NaN or infinite speeds usually mean dividing by zero.";
        } else {
          steered = result;
          steeringError = "";
        }
      } catch (RuntimeException | StackOverflowError e) {
        String message = report("SmartSteering.steer", e);
        if (!message.equals(steeringError)) {
          DriverStation.reportError("[Playground] " + message, e.getStackTrace());
        }
        steeringError = message;
      }
      steerMicros = (System.nanoTime() - start) / 1000.0;
    } else if (!smartOn && steering != null) {
      steeringError = "";
    }

    world.step(driving ? steered : new ChassisSpeeds(), allObstacles);
    publish();
  }

  // The dashboard's inputs ----------------------------------------------------------------------

  private void readDashboard() {
    input.setDashboardAxes(SmartDashboard.getNumberArray(KEYS, new double[0]));
    smartOn = SmartDashboard.getBoolean(SMART, true);

    double[] drawn = SmartDashboard.getNumberArray(DRAWN, new double[0]);
    if (!Arrays.equals(drawn, lastDrawn)) {
      lastDrawn = drawn;
      List<Rectangle2d> parsed = new ArrayList<>();
      for (int i = 0; i + 5 <= drawn.length && parsed.size() < MAX_DRAWN; i += 5) {
        double cx = drawn[i];
        double cy = drawn[i + 1];
        double xWidth = drawn[i + 2];
        double yWidth = drawn[i + 3];
        double rotation = drawn[i + 4];
        if (!Double.isFinite(cx + cy + xWidth + yWidth + rotation)
            || xWidth < 0.01
            || yWidth < 0.01) {
          continue;
        }
        parsed.add(
            new Rectangle2d(new Pose2d(cx, cy, Rotation2d.fromRadians(rotation)), xWidth, yWidth));
      }
      drawnObstacles = List.copyOf(parsed);
      rebuildObstacles();
    }

    String command = SmartDashboard.getString(COMMAND, "");
    if (!command.equals(lastCommand)) {
      lastCommand = command;
      String action = command.contains("#") ? command.substring(0, command.indexOf('#')) : command;
      if (action.equals("reset")) {
        world.reset(PlaygroundConstants.START_POSE);
      }
    }
  }

  private void rebuildObstacles() {
    List<Rectangle2d> all = new ArrayList<>(codeObstacles);
    all.addAll(drawnObstacles);
    all.addAll(walls);
    allObstacles = Collections.unmodifiableList(all);
  }

  // What the dashboard and AdvantageScope see ---------------------------------------------------

  private void publish() {
    Pose2d pose = world.pose();
    ChassisSpeeds current = world.fieldSpeeds();
    double[] sticks = input.sticks();

    Logger.recordOutput("Playground/Time", world.time());
    Logger.recordOutput(
        "Playground/State",
        new double[] {
          world.time(),
          pose.getX(),
          pose.getY(),
          pose.getRotation().getRadians(),
          current.vxMetersPerSecond,
          current.vyMetersPerSecond,
          current.omegaRadiansPerSecond,
          requested.vxMetersPerSecond,
          requested.vyMetersPerSecond,
          requested.omegaRadiansPerSecond,
          steered.vxMetersPerSecond,
          steered.vyMetersPerSecond,
          steered.omegaRadiansPerSecond,
          DriverStation.isTeleopEnabled() ? 1 : 0,
          smartOn ? 1 : 0,
          world.bumps(),
          world.touching() ? 1 : 0,
          steerMicros,
          world.resets(),
          world.contactTime(),
          world.distance(),
          world.hardestBump(),
          sticks[0],
          sticks[1],
          sticks[2],
        });

    double[] obstacles = new double[(codeObstacles.size() + drawnObstacles.size()) * 6];
    int i = 0;
    for (Rectangle2d obstacle : codeObstacles) {
      i = putObstacle(obstacles, i, obstacle, 0);
    }
    for (Rectangle2d obstacle : drawnObstacles) {
      i = putObstacle(obstacles, i, obstacle, 1);
    }
    Logger.recordOutput("Playground/Obstacles", obstacles);

    List<World.Bump> bumps = world.recentBumps();
    double[] bumpData = new double[bumps.size() * 4];
    for (int b = 0; b < bumps.size(); b++) {
      World.Bump bump = bumps.get(b);
      bumpData[b * 4] = bump.x();
      bumpData[b * 4 + 1] = bump.y();
      bumpData[b * 4 + 2] = bump.time();
      bumpData[b * 4 + 3] = bump.speed();
    }
    Logger.recordOutput("Playground/Bumps", bumpData);
    Logger.recordOutput("Playground/Debug/Shapes", Debug.shapes());
    Logger.recordOutput("Playground/Debug/Values", Debug.values());
    Logger.recordOutput("Playground/Error", steeringError);
    Logger.recordOutput("Playground/ObstacleError", obstacleError);

    // For AdvantageScope: put RobotPose on a 2D field, and ModuleStates on a swerve tab.
    Logger.recordOutput("Playground/RobotPose", pose);
    Logger.recordOutput("Playground/RequestedSpeeds", requested);
    Logger.recordOutput("Playground/SteeredSpeeds", steered);
    ChassisSpeeds robotRelative =
        ChassisSpeeds.fromFieldRelativeSpeeds(current, pose.getRotation());
    SwerveModuleState[] states = kinematics.toSwerveModuleStates(robotRelative);
    Logger.recordOutput("Playground/ModuleStates", states);
  }

  private static int putObstacle(double[] out, int i, Rectangle2d obstacle, int source) {
    out[i] = obstacle.getCenter().getX();
    out[i + 1] = obstacle.getCenter().getY();
    out[i + 2] = obstacle.getXWidth();
    out[i + 3] = obstacle.getYWidth();
    out[i + 4] = obstacle.getRotation().getRadians();
    out[i + 5] = source;
    return i + 6;
  }

  private static String config() {
    return String.format(
        Locale.ROOT,
        "{\"fieldLength\":%f,\"fieldWidth\":%f,\"robotLength\":%f,\"robotWidth\":%f,"
            + "\"maxSpeed\":%f,\"maxAngularSpeed\":%f,\"startX\":%f,\"startY\":%f,"
            + "\"startHeading\":%f}",
        PlaygroundConstants.FIELD_LENGTH,
        PlaygroundConstants.FIELD_WIDTH,
        PlaygroundConstants.ROBOT_LENGTH,
        PlaygroundConstants.ROBOT_WIDTH,
        PlaygroundConstants.MAX_SPEED,
        PlaygroundConstants.MAX_ANGULAR_SPEED,
        PlaygroundConstants.START_POSE.getX(),
        PlaygroundConstants.START_POSE.getY(),
        PlaygroundConstants.START_POSE.getRotation().getRadians());
  }

  private static ChassisSpeeds copy(ChassisSpeeds speeds) {
    return new ChassisSpeeds(
        speeds.vxMetersPerSecond, speeds.vyMetersPerSecond, speeds.omegaRadiansPerSecond);
  }

  /** A one-line description of an exception, with the line of student code it came from. */
  private static String report(String where, Throwable e) {
    Throwable cause =
        e instanceof ExceptionInInitializerError && e.getCause() != null ? e.getCause() : e;
    String message = cause.getClass().getSimpleName();
    if (cause.getMessage() != null) {
      message += ": " + cause.getMessage();
    }
    for (StackTraceElement frame : cause.getStackTrace()) {
      String name = frame.getClassName();
      if (name.startsWith("frc.robot.") && !name.startsWith("frc.robot.playground.")) {
        message += " (" + frame.getFileName() + ", line " + frame.getLineNumber() + ")";
        break;
      }
    }
    return where + " threw " + message;
  }
}
