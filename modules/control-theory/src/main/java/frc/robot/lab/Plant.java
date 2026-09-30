package frc.robot.lab;

/**
 * A simulated motor-driven mechanism: the "plant" the student's controller drives.
 *
 * <p>Every mechanism in the lab uses the same model that WPILib's feedforward classes and SysId
 * use, where the voltage splits into the parts that overcome friction, gravity, speed and
 * acceleration:
 *
 * <pre>
 *   volts = kS * sign(velocity) + kG * gravity(position) + kV * velocity + kA * acceleration
 * </pre>
 *
 * <p>Friction is Coulomb friction with sticking: a mechanism at rest stays at rest until the
 * voltage beats kS. The model is integrated in small substeps between robot loops, like a real
 * mechanism that keeps moving between them.
 */
final class Plant {
  enum Kind {
    FLYWHEEL("flywheel", "RPM"),
    STEERING("steering", "deg"),
    ELEVATOR("elevator", "m"),
    ARM("arm", "rad");

    final String id;
    final String units;

    Kind(String id, String units) {
      this.id = id;
      this.units = units;
    }
  }

  enum Gravity {
    NONE,
    /** An elevator: gravity pulls with the same force everywhere. */
    CONSTANT,
    /** An arm: gravity's torque is largest when horizontal (position 0 radians). */
    COSINE
  }

  private static final int SUBSTEPS = 20;

  final Kind kind;
  final double kS;
  final double kV;
  final double kA;
  final double kG;
  final Gravity gravity;
  final double minPosition;
  final double maxPosition;

  /** Position units per velocity unit per second: 1/60 for a flywheel (RPM to rotations). */
  private final double positionRate;

  /** Extra load while a ball is being shot, in volts, and how long a shot lasts. */
  final double shotVolts;

  static final double SHOT_SECONDS = 0.08;

  /** Extra gravity and inertia while carrying a payload. */
  private double payloadKg = 0.0;

  private double payloadKa = 0.0;

  private double position;
  private double velocity;
  private double shotTimeLeft = 0.0;

  private Plant(
      Kind kind,
      double kS,
      double kV,
      double kA,
      double kG,
      Gravity gravity,
      double minPosition,
      double maxPosition,
      double positionRate,
      double shotVolts) {
    this.kind = kind;
    this.kS = kS;
    this.kV = kV;
    this.kA = kA;
    this.kG = kG;
    this.gravity = gravity;
    this.minPosition = minPosition;
    this.maxPosition = maxPosition;
    this.positionRate = positionRate;
    this.shotVolts = shotVolts;
  }

  // The lab's mechanisms
  // ---------------------------------------------------------------------------

  /** A shooter flywheel: about 5900 RPM free speed, time constant 0.3 s. */
  static Plant flywheel() {
    return new Plant(
        Kind.FLYWHEEL,
        0.25,
        0.002,
        0.0006,
        0.0,
        Gravity.NONE,
        Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY,
        1.0 / 60.0,
        10.0);
  }

  /** A flywheel whose constants the student has to measure (the SysId challenge). */
  static Plant mysteryFlywheel() {
    return new Plant(
        Kind.FLYWHEEL,
        0.32,
        0.00175,
        0.00042,
        0.0,
        Gravity.NONE,
        Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY,
        1.0 / 60.0,
        5.0);
  }

  /** A swerve module's steering motor, in degrees: about 1500 degrees per second free speed. */
  static Plant steering() {
    return new Plant(
        Kind.STEERING,
        0.15,
        0.008,
        0.0008,
        0.0,
        Gravity.NONE,
        Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY,
        1.0,
        0.0);
  }

  /** An elevator from 0 to 1.6 meters: about 1.9 m/s free speed. */
  static Plant elevator() {
    return new Plant(Kind.ELEVATOR, 0.15, 6.0, 0.6, 0.45, Gravity.CONSTANT, 0.0, 1.6, 1.0, 0.0);
  }

  /** A single-jointed arm from -45 to 135 degrees, in radians. */
  static Plant arm() {
    return new Plant(
        Kind.ARM,
        0.1,
        1.8,
        0.12,
        0.9,
        Gravity.COSINE,
        Math.toRadians(-45),
        Math.toRadians(135),
        1.0,
        0.0);
  }

  // Simulation
  // -------------------------------------------------------------------------------------

  void reset(double position, double velocity) {
    this.position = position;
    this.velocity = velocity;
    shotTimeLeft = 0.0;
    payloadKg = 0.0;
    payloadKa = 0.0;
  }

  /** The true position. For steering it keeps counting past ±180 degrees. */
  double position() {
    return position;
  }

  double velocity() {
    return velocity;
  }

  /** The position a sensor reports: steering wraps into -180 to 180 degrees. */
  double reportedPosition() {
    return kind == Kind.STEERING ? wrapDegrees(position) : position;
  }

  /** The quantity the goal is for: speed for a flywheel, position for everything else. */
  double output() {
    return kind == Kind.FLYWHEEL ? velocity : reportedPosition();
  }

  /** How far the output is from the goal, taking the short way around for steering. */
  double error(double goal) {
    double error = goal - output();
    return kind == Kind.STEERING ? wrapDegrees(error) : error;
  }

  void shoot() {
    shotTimeLeft = SHOT_SECONDS;
  }

  boolean shooting() {
    return shotTimeLeft > 0;
  }

  void setPayload(boolean carrying) {
    payloadKg = carrying ? 0.35 : 0.0;
    payloadKa = carrying ? 0.25 : 0.0;
  }

  boolean hasPayload() {
    return payloadKg > 0;
  }

  /** Suddenly changes the velocity, like someone bumping the mechanism. */
  void bump(double deltaVelocity) {
    velocity += deltaVelocity;
  }

  /** Runs the mechanism for {@code seconds} with this voltage applied. */
  void step(double volts, double seconds) {
    double h = seconds / SUBSTEPS;
    for (int i = 0; i < SUBSTEPS; i++) {
      double load = shotTimeLeft > 0 ? shotVolts : 0.0;
      shotTimeLeft = Math.max(shotTimeLeft - h, 0.0);

      double gravityVolts =
          switch (gravity) {
            case NONE -> 0.0;
            case CONSTANT -> kG + payloadKg;
            case COSINE -> (kG + payloadKg) * Math.cos(position);
          };
      // Voltage left over to accelerate the mechanism, before friction.
      double drive = volts - gravityVolts - kV * velocity;
      // A shot's load always slows the flywheel down.
      drive -= Math.signum(velocity) * load;
      double inertia = kA + payloadKa;

      double next;
      if (velocity == 0.0) {
        // Stuck: static friction holds until the drive beats it.
        next = Math.abs(drive) <= kS ? 0.0 : (drive - Math.signum(drive) * kS) / inertia * h;
      } else {
        next = velocity + (drive - Math.signum(velocity) * kS) / inertia * h;
        // Friction stops the mechanism rather than pushing it backwards.
        if (Math.signum(next) != Math.signum(velocity)) {
          next = 0.0;
        }
      }
      velocity = next;
      position += velocity * positionRate * h;

      if (position <= minPosition) {
        position = minPosition;
        velocity = Math.max(velocity, 0.0);
      } else if (position >= maxPosition) {
        position = maxPosition;
        velocity = Math.min(velocity, 0.0);
      }
    }
  }

  static double wrapDegrees(double degrees) {
    double wrapped = ((degrees + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
    return wrapped == -180.0 ? 180.0 : wrapped;
  }
}
