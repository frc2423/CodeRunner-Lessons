package frc.robot.challenges;

import frc.robot.Constants;
import frc.robot.challenges.fromscratch.Pivot;
import frc.robot.challenges.fromscratch.PivotIO;
import frc.robot.challenges.fromscratch.PivotIOSim;
import frc.robot.lab.LabChallenge;

public class Challenge14FromScratch extends LabChallenge {
  private final Pivot pivot =
      new Pivot(Constants.currentMode == Constants.Mode.REPLAY ? new PivotIO() {} : new PivotIOSim());

  @Override
  public void configureBindings() {
    controller.a().onTrue(pivot.runOnce(() -> pivot.setSetpointDegrees(90.0)));
    controller.b().onTrue(pivot.runOnce(() -> pivot.setSetpointDegrees(0.0)));
  }
}
