# Control Theory Challenges

Learn to control real FRC mechanisms (a shooter flywheel, a swerve module, an elevator and an arm) by writing the controllers yourself and testing them on simulated versions. The challenges follow the free book *Controls Engineering in the FIRST Robotics Competition* by Tyler Veness, from bang-bang and PID all the way to LQR and Kalman filters.

Each challenge is its own class in `src/main/java/frc/robot/challenges/`, and you only need to change that one file. The class is a `Controller`: every 20 ms, its `calculate` method reads the mechanism's sensors and goal and returns a motor voltage.

Open the **Control Lab** tab next to AdvantageScope. Pick a challenge there to read the idea behind it, which file to edit, the code you might need, hints, and the book sections to read. The tab animates the mechanism, plots its response, and grades your controller.

## Steps

1. Click **Start** in the Driver Station. The dot at the top of the Control Lab tab turns green when the robot program is running.
2. Pick **Example: Open Loop** and click **Run trial**. Open `Challenge00OpenLoop.java` to see how a controller works.
3. Work through the challenges in order, starting with **1. Bang-Bang**. For each one:
   - Write your controller in the challenge's file.
   - Save, then click **Restart** in the Driver Station so the robot program is rebuilt with your code. The last thing you ran starts again by itself.
   - Tune your gains with the sliders (every `TunableNumber` gets one), and click **Run trial** again. Pass every check to complete the challenge; bonus checks earn stars.
4. Click **Sandbox** to play with your controller: set the goal yourself, shoot balls, add game pieces and bump the mechanism.
5. If your code crashes, the tab shows the error and the line it happened on. Fix it and restart.

The lab runs whether or not the robot is enabled. (On a real robot, motors only move when it is enabled.)

## Challenges

| # | Challenge | Mechanism | You'll practise |
| --- | --- | --- | --- |
| 0 | Example: Open Loop | Flywheel | plant, input, output |
| 1 | Bang-Bang | Flywheel | closed-loop control, error |
| 2 | Proportional Control | Flywheel | P controller, steady-state error |
| 3 | Feedforward | Flywheel | kS and kV, rejecting disturbances |
| 4 | PD Steering | Swerve module | derivative term, damping |
| 5 | Shortest Path | Swerve module | WPILib `PIDController`, continuous input |
| 6 | Fight Gravity | Elevator | gravity feedforward |
| 7 | Mystery Payload | Elevator | integral term, windup, I-zone |
| 8 | Arm Feedforward | Arm | `kG · cos(θ)`, radians |
| 9 | Motion Profile | Elevator | `TrapezoidProfile`, actuator saturation |
| 10 | Mystery Flywheel (SysId) | Flywheel | system identification |
| 11 | LQR | Flywheel | state-space models, Bryson's rule |
| 12 | Noisy Sensor | Flywheel | low-pass filters, lag |
| 13 | Kalman Filter | Flywheel | state observers |
| 14 | State-Space Elevator | Elevator | LQR + Kalman filter + profile together |

Challenges 11 and 13 use the constants you measure in challenge 10, so do that one first.

## Bonus

- Every challenge draws your own lines on the plot with `mechanism.plot("name", value)`. Plot your error, your feedforward, or your profile's setpoint to see what your controller is thinking.
- Everything the lab does is logged under `ControlLab/`, so you can also study it in AdvantageScope.
- Read the book: <https://file.tavsys.net/control/controls-engineering-in-frc.pdf>. Each challenge lists the sections that go with it.
