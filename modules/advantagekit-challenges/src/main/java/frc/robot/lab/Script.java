package frc.robot.lab;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.XboxController;
import java.util.ArrayList;
import java.util.List;

/**
 * How a challenge's trial goes: where the robot starts, when game pieces arrive, and what the
 * scripted driver does on the controller and when.
 */
final class Script {
  /** Buttons, by their {@link XboxController.Button} number. */
  static final int A = XboxController.Button.kA.value;

  static final int B = XboxController.Button.kB.value;
  static final int X = XboxController.Button.kX.value;
  static final int Y = XboxController.Button.kY.value;

  /** Axes, by their {@link XboxController.Axis} number. */
  static final int LEFT_X = XboxController.Axis.kLeftX.value;

  static final int LEFT_Y = XboxController.Axis.kLeftY.value;
  static final int RIGHT_X = XboxController.Axis.kRightX.value;

  private record Press(int button, double from, double to) {}

  private record Stick(int axis, double value, double from, double to) {}

  /** Something that happens once, at a moment in the trial. */
  record Event(double time, Runnable action) {}

  final double duration;
  Pose2d start = new Pose2d(3.0, 3.0, Rotation2d.kZero);
  final List<Event> events = new ArrayList<>();
  private final List<Press> presses = new ArrayList<>();
  private final List<Stick> sticks = new ArrayList<>();

  Script(double duration) {
    this.duration = duration;
  }

  Script start(Pose2d pose) {
    start = pose;
    return this;
  }

  /** A game piece arrives in the feeder at {@code time}. */
  Script piece(double time) {
    return at(time, () -> Hardware.world().loadPiece());
  }

  /** The gyro feels a short, sharp hit at {@code time}. */
  Script bump(double time, double rate, double seconds) {
    return at(time, () -> Hardware.world().bump(rate, seconds));
  }

  /** Something else happens at {@code time}. */
  Script at(double time, Runnable action) {
    events.add(new Event(time, action));
    events.sort((a, b) -> Double.compare(a.time(), b.time()));
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

  /** Pushes a stick to {@code value} from {@code from} to {@code to} seconds. */
  Script stick(int axis, double value, double from, double to) {
    sticks.add(new Stick(axis, value, from, to));
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
    controller.setScripted(buttons, axes);
  }
}
