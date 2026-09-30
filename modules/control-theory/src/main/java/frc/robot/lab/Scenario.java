package frc.robot.lab;

import java.util.ArrayList;
import java.util.List;

/**
 * The script of one trial: how long it lasts, where the mechanism starts, when the goal changes,
 * and what happens to the mechanism along the way.
 */
final class Scenario {
  enum EventType {
    /** A ball goes through the flywheel and slows it down. */
    SHOT,
    /** The mechanism picks up a heavy game piece. */
    PAYLOAD_ON,
    PAYLOAD_OFF,
    /** Something bumps the mechanism. */
    BUMP
  }

  record Event(double time, EventType type, double amount) {}

  private record GoalChange(double time, double goal) {}

  final double duration;
  double startPosition = 0.0;
  double startVelocity = 0.0;

  /** Standard deviation of the noise on the measured velocity and position. */
  double velocityNoise = 0.0;

  double positionNoise = 0.0;

  private final List<GoalChange> goals = new ArrayList<>();
  final List<Event> events = new ArrayList<>();

  Scenario(double duration) {
    this.duration = duration;
  }

  Scenario start(double position) {
    startPosition = position;
    return this;
  }

  /** From {@code time} on, the goal is {@code goal}. Add changes in time order. */
  Scenario goal(double time, double goal) {
    goals.add(new GoalChange(time, goal));
    return this;
  }

  Scenario shot(double time) {
    events.add(new Event(time, EventType.SHOT, 0.0));
    return this;
  }

  Scenario payload(double time, boolean carrying) {
    events.add(new Event(time, carrying ? EventType.PAYLOAD_ON : EventType.PAYLOAD_OFF, 0.0));
    return this;
  }

  Scenario bump(double time, double deltaVelocity) {
    events.add(new Event(time, EventType.BUMP, deltaVelocity));
    return this;
  }

  Scenario noise(double velocityStdDev, double positionStdDev) {
    velocityNoise = velocityStdDev;
    positionNoise = positionStdDev;
    return this;
  }

  double goalAt(double time) {
    double goal = goals.isEmpty() ? 0.0 : goals.get(0).goal();
    for (GoalChange change : goals) {
      if (time + 1e-9 >= change.time()) {
        goal = change.goal();
      }
    }
    return goal;
  }

  /** Times the goal changes, including the start. */
  List<Double> goalTimes() {
    return goals.stream().map(GoalChange::time).toList();
  }

  List<Double> eventTimes(EventType type) {
    return events.stream().filter(e -> e.type() == type).map(Event::time).toList();
  }
}
