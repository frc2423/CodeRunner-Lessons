package frc.robot.challenges;

import frc.robot.Constants;
import frc.robot.challenges.fastsensors.GyroIO;
import frc.robot.challenges.fastsensors.GyroIOSim;
import frc.robot.challenges.fastsensors.Heading;
import frc.robot.lab.LabChallenge;

/**
 * Challenge 13: Faster Than the Loop.
 *
 * <p>The robot code runs 50 times a second, and the gyro takes 250 samples a second. During this
 * trial the robot gets hit: a short, sharp bump that's over in a few milliseconds. Reading the gyro
 * once per loop can miss it completely.
 *
 * <p>Inputs don't have to be single values. Your work is in {@code challenges/fastsensors/}: get
 * every sample through the IO layer as an array, and add them all up to work out the heading.
 */
public class Challenge13FastSensors extends LabChallenge {
  private final Heading heading =
      new Heading(
          Constants.currentMode == Constants.Mode.REPLAY ? new GyroIO() {} : new GyroIOSim());

  @Override
  public void configureBindings() {}
}
