# Command Lab dashboard (source)

Source for the **Command Lab** tab of the `command-based-challenges` lesson.
It was started from CodeRunner's `templates/dashboard-react` (React + Vite +
TypeScript), like the Control Lab and LED Challenges dashboards.

This folder is **not** part of the lesson module. Students get only the built
output, which lives in `modules/command-based-challenges/dashboard/`.

## Work on it

```sh
bun install            # or: npm install
bun run dev            # no robot needed: see "Dev mock" below
```

## Rebuild the lesson copy

```sh
bun run build:lesson   # writes ../../modules/command-based-challenges/dashboard/
```

Commit the rebuilt `modules/command-based-challenges/dashboard/` together with
any source change. The tab is declared in
`modules/command-based-challenges/.coderunner/dashboards.json`.

## How the lesson is split

The robot program owns the simulation (the robot, the field, the score), each
trial's script (where the robot starts, which pieces roll in, what the scripted
driver presses and when) and how it's graded. The dashboard owns what the
student reads and sees.

| Where | What it holds |
| --- | --- |
| `modules/command-based-challenges/src/main/java/frc/robot/challenges/` | The student scaffolds, one `CommandChallenge` class per challenge. Challenges 11 and 12 also use `subsystems/Climber.java` and `commands/AimAtHub.java`. |
| `.../frc/robot/subsystems/` | The robot's subsystems. They call `lab/Hardware.java` where a real robot would talk to motor controllers. |
| `.../frc/robot/lab/Challenges.java` | Each challenge's trial script and checks (with thresholds). |
| `.../frc/robot/lab/World.java` | The simulation: drive, intake/indexer piece handling, shooter, climber, LEDs, shots and scoring. |
| `.../frc/robot/lab/CommandLab.java` | Runs the scheduler and the trials, swaps challenges, configures PathPlanner's `AutoBuilder`, and publishes everything below. |
| `.../frc/robot/lab/LabController.java` | The driver's `CommandXboxController`, reading the Driver Station, the on-screen controller, or a trial's script. |
| `src/challenges.ts` | Every challenge's text (idea, steps, the trial, code, hints, links). |
| `src/lab.ts` | The NetworkTables topics shared with `CommandLab.java`, and the hooks that read them. |
| `src/coderunner/devMock.ts` | The in-memory robot for `bun run dev`. |

Adding or renaming a challenge means changing three places together: the class
in `challenges/`, its entry in `Challenges.java`, and its entry in
`src/challenges.ts` (same `id`).

## Topics

| Topic | Direction | Meaning |
| --- | --- | --- |
| `/SmartDashboard/CommandLab/RunRequest` | dashboard → robot | `"<id>\|trial#<nonce>"` or `"<id>\|free#<nonce>"` loads a challenge; `"#<nonce>"` stops. |
| `/SmartDashboard/CommandLab/Virtual` | dashboard → robot | The tab's controller, so no gamepad is needed: on-screen clicks and drags merged with the keyboard (same keys as the Driver Station's keyboard mode, see `src/keyboard.ts`). `[button bits, d-pad angle, six axes]`, axes in `XboxController.Axis` order. `LabController.java` ORs it with the Driver Station's joystick. |
| `/SmartDashboard/CommandLab/FieldEvent` | dashboard → robot | `"rollin#<nonce>"` (a piece into the intake) or `"reset#<nonce>"`, outside trials only. |
| `/CommandLab/Catalog` | robot → dashboard | JSON, published once at startup: each challenge's mode, length and check labels. |
| `/CommandLab/Result` | robot → dashboard | JSON, when a trial ends: every check's outcome and the score. |
| `/AdvantageKit/RealOutputs/CommandLab/State` | robot → dashboard | The robot packed in one array; see `toState` in `src/lab.ts` and `state()` in `CommandLab.java`. The last value counts field resets, so the dashboard can tell runs apart. |
| `.../CommandLab/{Pieces,Shots,Path}` | robot → dashboard | Floor pieces `[x, y, …]`, recent shots (9 values each), and PathPlanner's active path. |
| `.../CommandLab/{Controller,Commands,Subsystems,Events}` | robot → dashboard | What the controller reads, the running commands, each subsystem's current and default command, and the last 80 command start/finish/interrupt events (tab-separated). |
| `.../CommandLab/{Running,Mode,Phase,Waiting,Notice,RunId,Error,Time,Duration}` | robot → dashboard | What's loaded, whether a trial is waiting for the robot to be enabled, and why it stopped. `Time` changes every loop, so the dashboard uses it to spot a robot stuck in an endless loop. |

A trial starts when the robot is enabled in the trial's mode (Teleop or
Autonomous); disabling it or switching modes stops the trial. The trial's
scripted driver replaces every other input. Time counts robot loops, so a trial
grades the same every time for the same code.

## Reference solutions and calibration

`reference/CalibrationTest.java` holds a solution for every challenge except
Match Play, plus deliberately naive ones (no `until`, no `waitUntil`, reading
the sticks once, two plain `whileTrue` LED triggers, no `debounce`…), and runs
them all through the real trial and grading code with simulated time. It also
builds a three-piece PathPlanner auto from the shipped paths. Use it after
changing the simulation, a script or a threshold.

It is kept out of the module so students don't see the solutions. To run it:

```sh
mkdir -p ../../modules/command-based-challenges/src/test/java/frc/robot/lab
cp reference/CalibrationTest.java ../../modules/command-based-challenges/src/test/java/frc/robot/lab/
cd ../../modules/command-based-challenges
./gradlew test -i | grep -E "^ *(OK|NOPE|PASS|FAIL|ALL)"
rm -r src/test
```

Every reference line should say `OK`. The naive attempts should say `NOPE`,
except "robot-relative, no deadband", which passes but loses both bonus stars.

## Dev mock

On `bun run dev`, `src/coderunner/devMock.ts` stands in for `CommandLab.java`
with a robot that drives a loop, intakes and shoots, plus matching command
events, so the tab can be built and styled without a robot. Its checks are
placeholders; Spin Up fails one on purpose, to show a failed result.

See [Custom Dashboards](https://mathewdunne.github.io/CodeRunner/lessons/custom-dashboards)
for the `window.coderunner` API.
