package frc.robot.lab;

import edu.wpi.first.math.Pair;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.event.EventLoop;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import java.util.HashMap;
import java.util.Map;
import org.littletonrobotics.junction.Logger;

/**
 * The driver's Xbox controller. It is a real {@link CommandXboxController}, so everything you write
 * with it works on a real robot too. In the lab it reads three things:
 *
 * <ul>
 *   <li>the gamepad or keyboard plugged into CodeRunner's Driver Station (port 0),
 *   <li>the dashboard's buttons, and the keyboard while the tab has focus, and
 *   <li>during a graded trial, the trial's scripted driver (and nothing else).
 * </ul>
 *
 * <p>Whatever it reads is logged as an input once per loop, and your commands see the logged value.
 * That's how a replay presses the same buttons at the same moments.
 *
 * <p>You don't need to edit this file.
 */
public class LabController extends CommandXboxController {
  public static final int PORT = 0;

  private boolean scripted = false;
  private int scriptButtons;
  private double[] scriptAxes = new double[6];

  private int virtualButtons;
  private double[] virtualAxes = new double[6];

  private final DriverInputsAutoLogged inputs = new DriverInputsAutoLogged();

  private final Map<EventLoop, Map<Integer, Trigger>> buttons = new HashMap<>();
  private final Map<EventLoop, Map<Integer, Trigger>> povTriggers = new HashMap<>();
  private final Map<EventLoop, Map<Pair<Integer, Double>, Trigger>> below = new HashMap<>();
  private final Map<EventLoop, Map<Pair<Integer, Double>, Trigger>> above = new HashMap<>();
  private final Map<EventLoop, Map<Pair<Integer, Double>, Trigger>> beyond = new HashMap<>();

  public LabController() {
    super(PORT);
  }

  // What a CommandXboxController reads, redirected to the logged inputs -------------------------

  @Override
  public Trigger button(int button, EventLoop loop) {
    return buttons
        .computeIfAbsent(loop, k -> new HashMap<>())
        .computeIfAbsent(button, k -> new Trigger(loop, () -> isPressed(k)));
  }

  @Override
  public Trigger pov(int pov, int angle, EventLoop loop) {
    return povTriggers
        .computeIfAbsent(loop, k -> new HashMap<>())
        .computeIfAbsent(pov * 3600 + angle, k -> new Trigger(loop, () -> inputs.pov == angle));
  }

  @Override
  public Trigger axisLessThan(int axis, double threshold, EventLoop loop) {
    return below
        .computeIfAbsent(loop, k -> new HashMap<>())
        .computeIfAbsent(
            Pair.of(axis, threshold), k -> new Trigger(loop, () -> getRawAxis(axis) < threshold));
  }

  @Override
  public Trigger axisGreaterThan(int axis, double threshold, EventLoop loop) {
    return above
        .computeIfAbsent(loop, k -> new HashMap<>())
        .computeIfAbsent(
            Pair.of(axis, threshold), k -> new Trigger(loop, () -> getRawAxis(axis) > threshold));
  }

  @Override
  public Trigger axisMagnitudeGreaterThan(int axis, double threshold, EventLoop loop) {
    return beyond
        .computeIfAbsent(loop, k -> new HashMap<>())
        .computeIfAbsent(
            Pair.of(axis, threshold),
            k -> new Trigger(loop, () -> Math.abs(getRawAxis(axis)) > threshold));
  }

  @Override
  public double getRawAxis(int axis) {
    return axis >= 0 && axis < inputs.axes.length ? inputs.axes[axis] : 0;
  }

  @Override
  public double getLeftX() {
    return getRawAxis(XboxController.Axis.kLeftX.value);
  }

  @Override
  public double getLeftY() {
    return getRawAxis(XboxController.Axis.kLeftY.value);
  }

  @Override
  public double getRightX() {
    return getRawAxis(XboxController.Axis.kRightX.value);
  }

  @Override
  public double getRightY() {
    return getRawAxis(XboxController.Axis.kRightY.value);
  }

  @Override
  public double getLeftTriggerAxis() {
    return getRawAxis(XboxController.Axis.kLeftTrigger.value);
  }

  @Override
  public double getRightTriggerAxis() {
    return getRawAxis(XboxController.Axis.kRightTrigger.value);
  }

  /** Whether a button (numbered like {@link XboxController.Button}) is pressed. */
  public boolean isPressed(int button) {
    return button >= 1 && button <= 16 && (inputs.buttons & (1L << (button - 1))) != 0;
  }

  // Used by the lab ------------------------------------------------------------------------------

  /** Reads this loop's controller state and logs it. In a replay, the log provides it instead. */
  void update() {
    if (!Logger.hasReplaySource()) {
      read();
    }
    Logger.processInputs("Lab/Driver", inputs);
  }

  private void read() {
    long bits = 0;
    double[] axes = new double[6];
    int pov = -1;
    if (scripted) {
      bits = scriptButtons;
      axes = scriptAxes.clone();
    } else {
      bits = virtualButtons;
      int stickButtons = DriverStation.getStickButtonCount(PORT);
      for (int button = 1; button <= Math.min(stickButtons, 16); button++) {
        if (DriverStation.getStickButton(PORT, button)) {
          bits |= 1L << (button - 1);
        }
      }
      int stickAxes = DriverStation.getStickAxisCount(PORT);
      for (int axis = 0; axis < 6; axis++) {
        double stick = axis < stickAxes ? DriverStation.getStickAxis(PORT, axis) : 0;
        axes[axis] = virtualAxes[axis] != 0 ? virtualAxes[axis] : stick;
      }
      pov = DriverStation.getStickPOVCount(PORT) > 0 ? DriverStation.getStickPOV(PORT, 0) : -1;
    }
    inputs.buttons = bits;
    inputs.axes = axes;
    inputs.pov = pov;
  }

  void setScripted(int buttons, double[] axes) {
    scripted = true;
    scriptButtons = buttons;
    scriptAxes = axes;
  }

  void useRealInputs() {
    scripted = false;
  }

  boolean isScripted() {
    return scripted;
  }

  /** The dashboard's buttons: [button bits, six axes]. */
  void setVirtual(double[] values) {
    if (values == null || values.length < 1) {
      virtualButtons = 0;
      virtualAxes = new double[6];
      return;
    }
    virtualButtons = (int) values[0];
    double[] axes = new double[6];
    for (int i = 0; i < 6 && i + 1 < values.length; i++) {
      axes[i] = Math.max(-1, Math.min(1, values[i + 1]));
    }
    virtualAxes = axes;
  }

  int buttonBits() {
    return (int) inputs.buttons;
  }
}
