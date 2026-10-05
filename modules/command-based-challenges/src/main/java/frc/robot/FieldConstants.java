package frc.robot;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;

/**
 * Where things are on the field, in meters, measured from the blue alliance's corner (the same
 * coordinates AdvantageScope and PathPlanner use). This lesson always plays on the blue side of a
 * 2026-sized field.
 */
public final class FieldConstants {
  public static final double FIELD_LENGTH = 16.541;
  public static final double FIELD_WIDTH = 8.069;

  /** The center of the blue hub, the goal you shoot game pieces into. */
  public static final Translation2d HUB = new Translation2d(4.626, 4.035);

  /** A game piece scores if it lands this close to the center of the hub. */
  public static final double HUB_SCORING_RADIUS = 0.75;

  /** The hub is solid: robots can't drive closer to its center than this. */
  public static final double HUB_RADIUS = 0.6;

  /** A good place to shoot from: 3 m straight out from the hub, facing it. */
  public static final Pose2d SHOOTING_SPOT = new Pose2d(1.626, 4.035, Rotation2d.kZero);

  /** The climbing bar. Your climber can only hook onto it when the robot is in its zone. */
  public static final Translation2d TOWER = new Translation2d(1.2, 7.0);

  /** How close to the tower the robot's center must be for the climber to hook on. */
  public static final double TOWER_ZONE_RADIUS = 0.8;

  /** Where the human player rolls new game pieces onto the field. */
  public static final Translation2d HUMAN_PLAYER_STATION = new Translation2d(0.9, 0.9);

  private FieldConstants() {}
}
