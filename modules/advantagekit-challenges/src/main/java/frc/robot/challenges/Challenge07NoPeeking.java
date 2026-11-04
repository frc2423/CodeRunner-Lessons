package frc.robot.challenges;

import frc.robot.Constants;
import frc.robot.challenges.nopeeking.AutoFeeder;
import frc.robot.challenges.nopeeking.AutoFeederIO;
import frc.robot.challenges.nopeeking.AutoFeederIOSim;
import frc.robot.lab.LabChallenge;
import frc.robot.subsystems.flywheel.Flywheel;

/**
 * Challenge 7: No Peeking.
 *
 * <p>This robot works! Holding A spins up the flywheel, and the feeder fires a piece as soon as the
 * flywheel is ready. Run the trial: the robot does the right thing, and the replay still comes out
 * different. Then look at the replay plot to see where it went wrong.
 *
 * <p>Your work is in {@code challenges/nopeeking/}. Find the places where {@code AutoFeeder} reads
 * the hardware itself, and fix them.
 */
public class Challenge07NoPeeking extends LabChallenge {
  private final Flywheel flywheel = Flywheel.create();
  private final AutoFeeder feeder =
      new AutoFeeder(
          Constants.currentMode == Constants.Mode.REPLAY
              ? new AutoFeederIO() {}
              : new AutoFeederIOSim());

  @Override
  public void configureBindings() {
    controller
        .a()
        .whileTrue(flywheel.startEnd(() -> flywheel.setTargetRpm(3000.0), flywheel::stop));
    controller.a().whileTrue(feeder.feedWhenReady());
  }
}
