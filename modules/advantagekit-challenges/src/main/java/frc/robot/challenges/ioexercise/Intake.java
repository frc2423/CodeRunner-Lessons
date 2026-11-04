package frc.robot.challenges.ioexercise;

import edu.wpi.first.wpilibj2.command.SubsystemBase;

/** The intake rollers. Step 3 of this challenge. */
public class Intake extends SubsystemBase {
  private final IntakeIO io;

  // TODO: Make a field called inputs, holding a new IntakeIOInputsAutoLogged. AdvantageKit makes
  // that class when your code builds, as soon as IntakeIOInputs has its @AutoLog. If the editor
  // can't find it, click Restart in the Driver Station to build.

  private double volts = 0.0;

  public Intake(IntakeIO io) {
    this.io = io;
  }

  @Override
  public void periodic() {
    // TODO: Update the inputs, send them to AdvantageKit under the name "Intake", and tell the IO
    // layer to run the motor at the voltage in the volts field.
  }

  public void setVolts(double volts) {
    this.volts = volts;
  }
}
