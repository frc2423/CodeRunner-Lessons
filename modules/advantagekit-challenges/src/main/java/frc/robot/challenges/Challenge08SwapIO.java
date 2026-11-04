package frc.robot.challenges;

import frc.robot.Constants;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.flywheel.Flywheel;
import frc.robot.subsystems.flywheel.FlywheelIO;
import frc.robot.subsystems.flywheel.FlywheelIOSim;

/**
 * Challenge 8: One Robot, Three Modes.
 *
 * <p>The same robot code runs in three very different places. {@code Constants.currentMode} says
 * which one: {@code REAL} on the roboRIO, {@code SIM} in a physics simulator, and {@code REPLAY}
 * during log replay. The only thing that needs to change is which IO layer the subsystem gets.
 *
 * <p>In a normal project this {@code switch} is in the {@code RobotContainer} constructor.
 */
public class Challenge08SwapIO extends LabChallenge {
  private final Flywheel flywheel = new Flywheel(createIO(Constants.currentMode));

  /** The right IO layer for each mode. */
  public static FlywheelIO createIO(Constants.Mode mode) {
    // TODO: Return a new FlywheelIOSpark() in REAL mode (import
    // frc.robot.subsystems.flywheel.FlywheelIOSpark), and a new FlywheelIOSim() in SIM mode. In
    // REPLAY mode, return a FlywheelIO with no hardware at all: new FlywheelIO() {}

    return new FlywheelIOSim();
  }

  @Override
  public void configureBindings() {
    controller
        .a()
        .whileTrue(flywheel.startEnd(() -> flywheel.setTargetRpm(3000.0), flywheel::stop));
  }
}
