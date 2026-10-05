package frc.robot.challenges;

import frc.robot.lab.CommandChallenge;
import frc.robot.subsystems.Drive;

/**
 * Challenge 4: Drive with the Sticks.
 *
 * <p>Give the drivetrain a <b>default command</b>: the command a subsystem runs whenever no other
 * command is using it. It should drive the robot from the controller, field-relative:
 *
 * <ul>
 *   <li>left stick up/down: drive forward/backward (away from or toward your driver station),
 *   <li>left stick left/right: drive left/right,
 *   <li>right stick left/right: turn left/right.
 * </ul>
 *
 * <p>Stick values go from -1 to 1. Pushing a stick up gives a <i>negative</i> Y value, and pushing
 * it left gives a <i>negative</i> X value. Multiply by {@link Drive#MAX_SPEED} and {@link
 * Drive#MAX_ANGULAR_SPEED} to get speeds.
 */
public class Challenge04DriveWithSticks extends CommandChallenge {
  @Override
  public void configureBindings() {
    // TODO: Set the drive's default command to a command that, every loop, reads the sticks and
    // calls drive.driveFieldRelative(xSpeed, ySpeed, rotationSpeed).
  }
}
