package frc.robot.lab;

import edu.wpi.first.math.Pair;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.event.EventLoop;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import java.util.HashMap;
import java.util.Map;

/**
 * The driver's Xbox controller. It is a real {@link CommandXboxController}, so everything you write
 * with it works on a real robot too. In the lab it reads three things:
 *
 * <ul>
 *   <li>the gamepad or keyboard plugged into CodeRunner's Driver Station (port 0),
 *   <li>the on-screen controller in the Command Lab dashboard, and
 *   <li>during a graded trial, the trial's scripted driver (and nothing else).
 * </ul>
 *
 * <p>You don't need to edit this file.
 */
public class LabController extends CommandXboxController {
  public static final int PORT = 0;

  /** Where the inputs come from right now. */
  enum Source {
    REAL,
    SCRIPT
  }

  private Source source = Source.REAL;
  private int scriptButtons;
  private double[] scriptAxes = new double[6];
  private int scriptPov = -1;

  private int virtualButtons;
  private double[] virtualAxes = new double[6];
  private int virtualPov = -1;

  private double rumble;

  private final Map<EventLoop, Map<Integer, Trigger>> buttons = new HashMap<>();
  private final Map<EventLoop, Map<Integer, Trigger>> povTriggers = new HashMap<>();
  private final Map<EventLoop, Map<Pair<Integer, Double>, Trigger>> below = new HashMap<>();
  private final Map<EventLoop, Map<Pair<Integer, Double>, Trigger>> above = new HashMap<>();
  private final Map<EventLoop, Map<Pair<Integer, Double>, Trigger>> beyond = new HashMap<>();

  public LabController() {
    super(PORT);
  }

  // What a CommandXboxController reads, redirected to the lab's inputs ------------------------

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
        .computeIfAbsent(pov * 3600 + angle, k -> new Trigger(loop, () -> getPov() == angle));
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
    if (axis < 0 || axis >= 6) {
      return 0;
    }
    if (source == Source.SCRIPT) {
      return scriptAxes[axis];
    }
    if (virtualAxes[axis] != 0) {
      return virtualAxes[axis];
    }
    return DriverStation.getStickAxisCount(PORT) > axis
        ? DriverStation.getStickAxis(PORT, axis)
        : 0;
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

  @Override
  public void setRumble(RumbleType type, double value) {
    rumble = Math.max(0, Math.min(1, value));
    super.setRumble(type, value);
  }

  /** Whether a button (numbered like {@link XboxController.Button}) is pressed. */
  public boolean isPressed(int button) {
    if (button < 1 || button > 16) {
      return false;
    }
    int bit = 1 << (button - 1);
    if (source == Source.SCRIPT) {
      return (scriptButtons & bit) != 0;
    }
    if ((virtualButtons & bit) != 0) {
      return true;
    }
    return DriverStation.getStickButtonCount(PORT) >= button
        && DriverStation.getStickButton(PORT, button);
  }

  /** The d-pad angle in degrees (0 = up, clockwise), or -1 when it isn't pressed. */
  public int getPov() {
    if (source == Source.SCRIPT) {
      return scriptPov;
    }
    if (virtualPov >= 0) {
      return virtualPov;
    }
    return DriverStation.getStickPOVCount(PORT) > 0 ? DriverStation.getStickPOV(PORT, 0) : -1;
  }

  // Used by the lab ------------------------------------------------------------------------------

  void setScripted(int buttons, double[] axes, int pov) {
    source = Source.SCRIPT;
    scriptButtons = buttons;
    scriptAxes = axes;
    scriptPov = pov;
  }

  void useRealInputs() {
    source = Source.REAL;
  }

  Source source() {
    return source;
  }

  /** The dashboard's on-screen controller: [button bits, d-pad, six axes]. */
  void setVirtual(double[] values) {
    if (values == null || values.length < 2) {
      virtualButtons = 0;
      virtualPov = -1;
      virtualAxes = new double[6];
      return;
    }
    virtualButtons = (int) values[0];
    virtualPov = (int) values[1];
    double[] axes = new double[6];
    for (int i = 0; i < 6 && i + 2 < values.length; i++) {
      axes[i] = Math.max(-1, Math.min(1, values[i + 2]));
    }
    virtualAxes = axes;
  }

  double rumble() {
    return rumble;
  }

  void resetRumble() {
    rumble = 0;
    super.setRumble(RumbleType.kBothRumble, 0);
  }

  /** Every button's state as bits, button 1 (A) in bit 0. */
  int buttonBits() {
    int bits = 0;
    for (int button = 1; button <= 10; button++) {
      if (isPressed(button)) {
        bits |= 1 << (button - 1);
      }
    }
    return bits;
  }
}
