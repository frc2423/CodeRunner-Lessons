# Command-Based Challenges

Learn WPILib's command-based framework by programming a simulated competition robot: a swerve drive, an intake, an indexer with a beam break sensor, a shooter, LEDs, and a climber you write yourself. The challenges start with one button and one command, and build up to triggers, interrupts, your own subsystems and command classes, PathPlanner autonomous routines, and a match you drive yourself.

Each challenge is its own class in `src/main/java/frc/robot/challenges/`. A challenge is a small RobotContainer: it gets the robot's subsystems (`drive`, `intake`, `indexer`, `shooter`, `leds`) and the driver's `controller`, and binds commands in `configureBindings()`.

Open the **Command Lab** tab next to AdvantageScope. Pick a challenge there to read the idea behind it, which file to edit, the code you might need, and hints. The tab shows the field from above, the robot from the side, the controller, and a **command timeline** that shows every command as it runs, which subsystems it uses, and what interrupted it.

## Steps

1. Click **Start** in the Driver Station. The dot at the top of the Command Lab tab turns green when the robot program is running.
2. Pick **Example: Hello, Commands**, click **Run trial**, then **Enable** the robot in **Teleop**. A scripted driver presses the buttons and the trial is graded. Open `Challenge00Example.java` to see how it works.
3. Work through the challenges in order, starting with **1. Spin Up**. For each one:
   - Write your code in the challenge's file.
   - Save, then click **Restart** in the Driver Station so the robot program is rebuilt with your code. The last thing you ran loads again by itself.
   - Click **Run trial** and enable the robot in the mode the challenge asks for (Teleop, or Autonomous for challenges 13 and 14). Pass every check to complete the challenge; bonus checks earn stars.
4. Click **Free play** to drive the robot yourself with a gamepad, the keyboard, or the controller drawn in the tab. **Roll in a piece** gives you something to pick up.
5. If your code crashes, the tab shows the error and the line it happened on. Fix it and restart.

Commands only run while the robot is enabled, just like on a real robot.

## Challenges

| # | Challenge | You'll practise |
| --- | --- | --- |
| 0 | Example: Hello, Commands | subsystems, commands, triggers |
| 1 | Spin Up | `runOnce`, `onTrue`, requirements |
| 2 | Hold to Intake | `whileTrue`, `startEnd` |
| 3 | Toggle | `toggleOnTrue`, interrupting |
| 4 | Drive with the Sticks | default commands, deadband, field-relative |
| 5 | Intake Until Loaded | two requirements, `until`, `unless` |
| 6 | Shoot! | `sequence`, `waitUntil` |
| 7 | Ready, Set, Shoot | `parallel`, `deadline`, `deadlineFor` |
| 8 | Who's in Charge? | interrupt behavior, `cancel`, `finallyDo` |
| 9 | LED Signals | triggers from sensors, combining triggers |
| 10 | Smart Triggers | `and`, `negate`, `debounce` |
| 11 | Write a Subsystem | the climber (`subsystems/Climber.java`) |
| 12 | Write a Command Class | `AimAtHub` (`commands/AimAtHub.java`), `PIDController` |
| 13 | Timed Autonomous | `getAutonomousCommand`, `withTimeout` |
| 14 | PathPlanner Autonomous | named commands, the PathPlanner tab |
| 15 | Match Play | everything, in a one-minute match you drive |

Challenge 15 uses what you wrote in the earlier challenges, so copy your favorite commands over.

## Bonus

- Name your commands with `.withName("...")`: the timeline (and a few bonus checks) use the names.
- Everything the lab does is logged, so you can also study a run in AdvantageScope: put `CommandLab/RobotPose` on a 2D field, and graph `Shooter/RPM` or `Intake/RollerCurrent`.
- In the PathPlanner tab, add event markers to a path so the robot intakes while it drives, and build a "Three Piece" auto.
