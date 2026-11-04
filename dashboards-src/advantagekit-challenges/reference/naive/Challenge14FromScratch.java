package frc.robot.challenges;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.LabChallenge;

// NAIVE: crashes in its periodic, the most common beginner bug: a field that was never set.
public class Challenge14FromScratch extends LabChallenge {
  private static class Broken extends SubsystemBase {
    private String name;

    @Override
    public void periodic() {
      name.length();
    }
  }

  private final Broken broken = new Broken();

  @Override
  public void configureBindings() {}
}
