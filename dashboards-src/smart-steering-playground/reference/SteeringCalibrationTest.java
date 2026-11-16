package frc.robot.playground;

import static org.junit.jupiter.api.Assertions.*;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rectangle2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import frc.robot.Obstacles;
import frc.robot.SmartSteering;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleFunction;
import org.junit.jupiter.api.Test;

/**
 * Checks that the task is solvable: drives scripted runs with no steering, the student scaffold,
 * and the reference solution, and counts how often the robot drives into an obstacle or past a
 * field wall. The playground itself doesn't detect that (nothing is solid), so the counting is
 * done here with {@link RefGeometry}. See the dashboard README for how to run it.
 */
class SteeringCalibrationTest {
  interface Steering {
    ChassisSpeeds steer(
        ChassisSpeeds requested, Pose2d pose, ChassisSpeeds current, List<Rectangle2d> obstacles);
  }

  /** A finished run: where the robot ended up, how often it hit something, and its closest call. */
  record Run(World world, int hits, double minGap) {}

  static List<Rectangle2d> obstacles() {
    List<Rectangle2d> all = new ArrayList<>(Obstacles.create());
    all.addAll(World.walls());
    return all;
  }

  static Run drive(
      Pose2d start, double seconds, DoubleFunction<ChassisSpeeds> driver, Steering steering) {
    List<Rectangle2d> obstacles = obstacles();
    World world = new World();
    world.reset(start);
    boolean[] insideLast = new boolean[obstacles.size()];
    int hits = 0;
    double minGap = Double.POSITIVE_INFINITY;
    for (double t = 0; t < seconds; t += PlaygroundConstants.LOOP_PERIOD) {
      ChassisSpeeds requested = driver.apply(t);
      Debug.clear();
      ChassisSpeeds out =
          steering == null
              ? requested
              : steering.steer(requested, world.pose(), world.fieldSpeeds(), obstacles);
      world.step(out);
      Rectangle2d robot = RefGeometry.robotFootprint(world.pose());
      for (int i = 0; i < obstacles.size(); i++) {
        boolean inside = RefGeometry.overlaps(robot, obstacles.get(i));
        if (inside && !insideLast[i]) {
          hits++;
        }
        insideLast[i] = inside;
        minGap = Math.min(minGap, RefGeometry.distance(robot, obstacles.get(i)));
      }
    }
    return new Run(world, hits, minGap);
  }

  static final Steering NONE = null;
  static final Steering SCAFFOLD = new SmartSteering()::steer;
  static final Steering REFERENCE = new ReferenceSmartSteering()::steer;

  static void report(String name, Run none, Run scaffold, Run reference) {
    System.out.printf(
        "%-24s none: %2d hits | scaffold: %2d hits | reference: %2d hits, min gap %.3f m, ended at"
            + " (%.2f, %.2f), drove %.1f m%n",
        name,
        none.hits(),
        scaffold.hits(),
        reference.hits(),
        reference.minGap(),
        reference.world().pose().getX(),
        reference.world().pose().getY(),
        reference.world().distance());
  }

  // RefGeometry ------------------------------------------------------------------------------------

  @Test
  void geometry() {
    Rectangle2d a = new Rectangle2d(new Translation2d(0, 0), new Translation2d(1, 1));
    Rectangle2d b = new Rectangle2d(new Translation2d(0.9, 0.2), new Translation2d(2, 0.8));
    assertTrue(RefGeometry.overlaps(a, b));
    Translation2d push = RefGeometry.separation(a, b);
    assertEquals(-0.1, push.getX(), 1e-9);
    assertEquals(0, push.getY(), 1e-9);
    Rectangle2d c = new Rectangle2d(new Translation2d(3, 0), new Translation2d(4, 1));
    assertFalse(RefGeometry.overlaps(a, c));
    assertEquals(2, RefGeometry.distance(a, c), 1e-9);
    Rectangle2d diamond = new Rectangle2d(new Pose2d(2.5, 0.5, Rotation2d.fromDegrees(45)), 1, 1);
    assertEquals(1.5 - Math.sqrt(0.5), RefGeometry.distance(a, diamond), 1e-9);
    // Touching edges don't overlap.
    Rectangle2d d = new Rectangle2d(new Translation2d(1, 0), new Translation2d(2, 1));
    assertFalse(RefGeometry.overlaps(a, d));
  }

  // The playground's physics -----------------------------------------------------------------------

  @Test
  void robotDrivesThroughObstacles() {
    // Straight through the long obstacle at x 4.5 to 5.0 and out the other side.
    Run run = drive(PlaygroundConstants.START_POSE, 1.5, t -> new ChassisSpeeds(4.5, 0, 0), NONE);
    double back = run.world().pose().getX() - PlaygroundConstants.ROBOT_LENGTH / 2;
    System.out.printf("Through the long obstacle: back at %.3f, %d hits%n", back, run.hits());
    assertTrue(back > 5.0, "should come out the other side");
    assertEquals(1, run.hits());
  }

  @Test
  void robotStaysOnTheField() {
    Run run = drive(PlaygroundConstants.START_POSE, 2, t -> new ChassisSpeeds(-4.5, -4.5, 0), NONE);
    Pose2d pose = run.world().pose();
    System.out.printf("Into the corner: (%.3f, %.3f)%n", pose.getX(), pose.getY());
    assertEquals(PlaygroundConstants.ROBOT_WIDTH / 2, pose.getX(), 1e-9);
    assertEquals(PlaygroundConstants.ROBOT_WIDTH / 2, pose.getY(), 1e-9);
    assertEquals(0, run.world().fieldSpeeds().vxMetersPerSecond, 1e-9);
  }

  @Test
  void accelerationIsLimited() {
    World world = new World();
    world.step(new ChassisSpeeds(4.5, 0, 0));
    assertEquals(
        PlaygroundConstants.MAX_ACCELERATION * PlaygroundConstants.LOOP_PERIOD,
        world.fieldSpeeds().vxMetersPerSecond,
        1e-9);
  }

  // Steering ---------------------------------------------------------------------------------------

  @Test
  void steeringScenarios() {
    record Scenario(
        String name, Pose2d start, double seconds, DoubleFunction<ChassisSpeeds> driver) {}
    List<Scenario> scenarios =
        List.of(
            new Scenario(
                "straight at the wall",
                PlaygroundConstants.START_POSE,
                4,
                t -> new ChassisSpeeds(4.5, 0, 0)),
            new Scenario(
                "diagonal at the wall",
                PlaygroundConstants.START_POSE,
                4,
                t -> new ChassisSpeeds(3.2, 3.2, 0)),
            new Scenario(
                "into a field corner",
                new Pose2d(2, 2, Rotation2d.kZero),
                3,
                t -> new ChassisSpeeds(-3, -3, 0)),
            new Scenario(
                "spinning along a wall",
                new Pose2d(2, 0.6, Rotation2d.kZero),
                4,
                t -> new ChassisSpeeds(3, -1, 3)),
            new Scenario(
                "at the tilted barrier",
                new Pose2d(12, 3.1, Rotation2d.kZero),
                3,
                t -> new ChassisSpeeds(0, 4.5, 0)),
            new Scenario(
                "through the gap",
                new Pose2d(10.5, 3.1, Rotation2d.kZero),
                3,
                t -> new ChassisSpeeds(3, 0, 0)),
            new Scenario(
                "back off the wall",
                new Pose2d(3.6, 4, Rotation2d.kZero),
                2,
                t -> new ChassisSpeeds(t < 1 ? 4 : -4, 0, 0)));
    int referenceHits = 0;
    for (Scenario s : scenarios) {
      Run none = drive(s.start(), s.seconds(), s.driver(), NONE);
      Run scaffold = drive(s.start(), s.seconds(), s.driver(), SCAFFOLD);
      Run reference = drive(s.start(), s.seconds(), s.driver(), REFERENCE);
      report(s.name(), none, scaffold, reference);
      referenceHits += reference.hits();
    }
    assertEquals(0, referenceHits);

    // Smart steering mustn't trap the robot: straight at the wall, then straight back.
    Run back =
        drive(
            new Pose2d(3.6, 4, Rotation2d.kZero),
            2,
            t -> new ChassisSpeeds(t < 1 ? 4 : -4, 0, 0),
            REFERENCE);
    assertTrue(back.world().pose().getX() < 3.0, "should drive back away from the wall");
    // Diagonal at the wall should slide, not stop.
    Run slide =
        drive(
            PlaygroundConstants.START_POSE, 4, t -> new ChassisSpeeds(3.2, 3.2, 0), REFERENCE);
    assertTrue(slide.world().pose().getY() > 6.5, "should slide along the wall");
  }

  @Test
  void randomDriving() {
    int none = 0;
    int reference = 0;
    for (int seed = 0; seed < 20; seed++) {
      none += drive(PlaygroundConstants.START_POSE, 60, randomDriver(seed), NONE).hits();
      reference += drive(PlaygroundConstants.START_POSE, 60, randomDriver(seed), REFERENCE).hits();
    }
    System.out.printf(
        "Random driving, 20 minutes: no steering %d hits, reference %d hits%n", none, reference);
    assertTrue(none > 300);
    // The reference still grazes things now and then when squeezed between two obstacles at once.
    assertTrue(reference <= none / 20, "reference should avoid at least 95% of the hits");
  }

  static DoubleFunction<ChassisSpeeds> randomDriver(long seed) {
    Random random = new Random(seed);
    double[] current = new double[3];
    double[] next = {0};
    return t -> {
      if (t >= next[0]) {
        double angle = random.nextDouble() * 2 * Math.PI;
        double speed = random.nextDouble() < 0.2 ? 0 : 1.5 + random.nextDouble() * 3;
        current[0] = Math.cos(angle) * speed;
        current[1] = Math.sin(angle) * speed;
        current[2] = random.nextDouble() < 0.5 ? 0 : (random.nextDouble() * 2 - 1) * 4;
        next[0] = t + 0.4 + random.nextDouble() * 1.5;
      }
      return new ChassisSpeeds(current[0], current[1], current[2]);
    };
  }
}
