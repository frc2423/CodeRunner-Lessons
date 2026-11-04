# AdvantageKit Challenges

Learn [AdvantageKit](https://docs.advantagekit.org/) by programming a simulated command-based robot: a flywheel, a feeder with a beam break, a drivetrain, a fast gyro and a pivot. The challenges start with one line of logging, and build up to IO layers, deterministic code, and solving a mystery from a real match log.

The big idea is **log replay**. If your code logs every input it reads, you can run it again later, with no robot at all, and it does exactly what it did before. So from challenge 5 on, each trial is replayed for real: the lab starts a second copy of your program in AdvantageKit's `REPLAY` mode, feeds it the trial's log, and compares the two runs.

Each challenge is its own class in `src/main/java/frc/robot/challenges/`, a small RobotContainer with the driver's `controller`. Some have a folder of their own too.

Open the **AdvantageKit Lab** tab next to AdvantageScope. Pick a challenge there to read the idea behind it, which files to edit, the code you might need, and hints. The tab only shows what that challenge needs: what's in your log, plots, the buttons it uses, a field that draws your logged poses, or how your replay compared with the real run.

## Steps

1. Click **Start** in the Driver Station. The dot at the top of the AdvantageKit Lab tab turns green when the robot program is running.
2. Pick **Example: Hello, Logger**, click **Run trial**, then **Enable** the robot in **Teleop**. A scripted driver presses the buttons and the trial is graded. Open `Challenge00Example.java` to see how it works.
3. Work through the challenges in order, starting with **1. Your First Output**. For each one:
   - Write your code in the challenge's files.
   - Save, then click **Restart** in the Driver Station so the robot program is rebuilt with your code. The last thing you ran loads again by itself.
   - Click **Run trial** and enable the robot in **Teleop**. Challenges 9 and 15 replay a match log instead: click **Replay the match**, with no need to enable anything.
   - Pass every check to complete the challenge. Bonus checks earn stars.
4. Click **Free play** to press the buttons yourself, then **Enable** the robot (commands don't run while it's disabled). You don't need a gamepad: click the buttons in the tab, or click the tab and use the keyboard.

   | Keys | Controller |
   | --- | --- |
   | K L J I | A B X Y |
   | W A S D | left stick (move) |
   | left and right arrow keys | right stick (turn) |

   These are the same keys as the Driver Station's keyboard mode. A real gamepad works too.
5. If your code crashes, the tab shows the error and the line it happened on. Fix it and restart.

## Challenges

| # | Challenge | You'll practise |
| --- | --- | --- |
| 0 | Example: Hello, Logger | `Logger.recordOutput`, keys |
| 1 | Your First Output | outputs, values persist, units |
| 2 | Log Every Loop | `periodic()`, booleans, enums |
| 3 | Log a Pose | `Pose2d`, arrays, structs |
| 4 | Let the Annotation Do It | `@AutoLogOutput` |
| 5 | Process Inputs | `updateInputs`, `Logger.processInputs` |
| 6 | Write an IO Layer | IO interfaces, `@AutoLog` |
| 7 | No Peeking | reading a failed replay, keeping hardware in the IO layer |
| 8 | One Robot, Three Modes | `Constants.currentMode`, `REAL`, `SIM`, `REPLAY` |
| 9 | Log It in Hindsight | replaying a match log, `RealOutputs` and `ReplayOutputs` |
| 10 | Same Answer Every Time | `Timer.getTimestamp`, seeded random |
| 11 | Dashboard Inputs | `LoggedNetworkNumber`, `LoggedDashboardChooser` |
| 12 | Log the Scheduler | `onCommandInitialize`, `onCommandFinish`, `onCommandInterrupt` |
| 13 | Faster Than the Loop | array inputs, threads stay in the IO layer |
| 14 | From Scratch | a whole subsystem, with a perfect replay |
| 15 | The Qual 17 Mystery | debugging from a log, proving a fix |

## Bonus

- Everything in your log is also in the **AdvantageScope** tab while the program runs: outputs under `AdvantageKit/RealOutputs/`, and inputs under `AdvantageKit/`. Try a line graph, or drag `Odometry/Robot` onto a 2D field.
- After a replay, the logs are in the `logs/` folder: `last-run.wpilog` is what was recorded, and `last-run_sim.wpilog` holds both runs, as `RealOutputs` and `ReplayOutputs`.
- Read `Robot.java`. That setup, with its three modes, is what every AdvantageKit project starts from.
- Read the [AdvantageKit docs](https://docs.advantagekit.org/), and the [template projects](https://docs.advantagekit.org/getting-started/template-projects) to see these same patterns on a full robot.
