package frc.robot.lab;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.XboxController;
import frc.robot.FieldConstants;
import java.util.ArrayList;
import java.util.List;

/**
 * How a challenge's trial goes: where the robot starts, which game pieces are where, which mode the
 * robot must be enabled in, and what the scripted driver does on the controller and when.
 */
final class Script {
  enum Mode {
    TELEOP("Teleop"),
    AUTO("Autonomous");

    final String label;

    Mode(String label) {
      this.label = label;
    }
  }

  /** Buttons, by their {@link XboxController.Button} number. */
  static final int A = XboxController.Button.kA.value;

  static final int B = XboxController.Button.kB.value;
  static final int X = XboxController.Button.kX.value;
  static final int Y = XboxController.Button.kY.value;
  static final int LB = XboxController.Button.kLeftBumper.value;
  static final int RB = XboxController.Button.kRightBumper.value;

  /** Axes, by their {@link XboxController.Axis} number. */
  static final int LEFT_X = XboxController.Axis.kLeftX.value;

  static final int LEFT_Y = XboxController.Axis.kLeftY.value;
  static final int LEFT_TRIGGER = XboxController.Axis.kLeftTrigger.value;
  static final int RIGHT_TRIGGER = XboxController.Axis.kRightTrigger.value;
  static final int RIGHT_X = XboxController.Axis.kRightX.value;

  private record Press(int button, double from, double to) {}

  private record Stick(int axis, double value, double from, double to) {}

  private record Pov(int angle, double from, double to) {}

  double duration;
  Mode mode = Mode.TELEOP;
  /** The student drives in this trial; nothing is scripted. */
  boolean driverControlled = false;

  Pose2d start = FieldConstants.SHOOTING_SPOT;
  boolean preload = false;
  boolean humanPlayer = false;
  final List<Translation2d> pieces = new ArrayList<>();
  final List<Double> rollIns = new ArrayList<>();
  final List<Double> jams = new ArrayList<>();
  private final List<Press> presses = new ArrayList<>();
  private final List<Stick> sticks = new ArrayList<>();
  private final List<Pov> povs = new ArrayList<>();

  Script(double duration) {
    this.duration = duration;
  }

  /** A free-play field: the robot at the shooting spot and pieces to pick up. */
  static Script freePlay() {
    return new Script(0)
        .piece(2.6, 4.035)
        .piece(2.9, 5.9)
        .piece(2.9, 2.17)
        .piece(6.8, 4.035)
        .piece(6.2, 6.4)
        .piece(6.2, 1.7)
        .humanPlayer();
  }

  Script withDuration(double seconds) {
    duration = seconds;
    return this;
  }

  Script autonomous() {
    mode = Mode.AUTO;
    return this;
  }

  Script driverControlled() {
    driverControlled = true;
    return this;
  }

  Script start(Pose2d pose) {
    start = pose;
    return this;
  }

  /** The robot starts holding a piece at the beam break. */
  Script preload() {
    preload = true;
    return this;
  }

  Script piece(double x, double y) {
    pieces.add(new Translation2d(x, y));
    return this;
  }

  /** At {@code time}, a human player rolls a piece right into the front of the robot. */
  Script rollIn(double time) {
    rollIns.add(time);
    return this;
  }

  /** From {@code time}, the next piece picked up jams in the intake. */
  Script jam(double time) {
    jams.add(time);
    return this;
  }

  Script humanPlayer() {
    humanPlayer = true;
    return this;
  }

  /** Holds a button from {@code from} to {@code to} seconds. */
  Script hold(int button, double from, double to) {
    presses.add(new Press(button, from, to));
    return this;
  }

  /** A quick press: held for 0.1 s. */
  Script tap(int button, double at) {
    return hold(button, at, at + 0.1);
  }

  /** Pushes a stick (or squeezes a trigger) to {@code value} from {@code from} to {@code to}. */
  Script stick(int axis, double value, double from, double to) {
    sticks.add(new Stick(axis, value, from, to));
    return this;
  }

  /** Squeezes a trigger all the way for 0.1 s. */
  Script squeeze(int axis, double at) {
    return stick(axis, 1.0, at, at + 0.1);
  }

  /** Presses the d-pad at {@code angle} (0 = up, 180 = down) from {@code from} to {@code to}. */
  Script pov(int angle, double from, double to) {
    povs.add(new Pov(angle, from, to));
    return this;
  }

  /** Writes the scripted controller's state at {@code time} into the controller. */
  void apply(double time, LabController controller) {
    int buttons = 0;
    for (Press press : presses) {
      if (time >= press.from - 1e-9 && time < press.to - 1e-9) {
        buttons |= 1 << (press.button - 1);
      }
    }
    double[] axes = new double[6];
    for (Stick stick : sticks) {
      if (time >= stick.from - 1e-9 && time < stick.to - 1e-9) {
        axes[stick.axis] = stick.value;
      }
    }
    int pov = -1;
    for (Pov p : povs) {
      if (time >= p.from - 1e-9 && time < p.to - 1e-9) {
        pov = p.angle;
      }
    }
    controller.setScripted(buttons, axes, pov);
  }
}
