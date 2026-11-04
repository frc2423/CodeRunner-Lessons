# AdvantageKit Lab dashboard (source)

Source for the **AdvantageKit Lab** tab of the `advantagekit-challenges` lesson.
It was started from CodeRunner's `templates/dashboard-react` (React + Vite +
TypeScript), like the Command Lab and Control Lab dashboards.

This folder is **not** part of the lesson module. Students get only the built
output, which lives in `modules/advantagekit-challenges/dashboard/`.

## Work on it

```sh
bun install            # or: npm install
bun run dev            # no robot needed: see "Dev mock" below
```

## Rebuild the lesson copy

```sh
bun run build:lesson   # writes ../../modules/advantagekit-challenges/dashboard/
```

Commit the rebuilt `modules/advantagekit-challenges/dashboard/` together with
any source change. The tab is declared in
`modules/advantagekit-challenges/.coderunner/dashboards.json`.

## How the lesson is split

The robot program owns the simulation, each trial's script, how it's graded, and
the replays. The dashboard owns what the student reads and sees. Each challenge
shows only the panels it needs: see `panels` in `src/challenges.ts`.

| Where | What it holds |
| --- | --- |
| `modules/advantagekit-challenges/src/main/java/frc/robot/challenges/` | The student scaffolds, one `LabChallenge` class per challenge, some with a folder of their own. |
| `.../frc/robot/subsystems/` | A finished flywheel, feeder and drivetrain, with IO layers. |
| `.../frc/robot/lab/Hardware.java` | The stand-in for vendor libraries, which the IO layers call. In a replay it's gone, and it notes who tried to use it. |
| `.../frc/robot/lab/World.java` | The simulated robot. |
| `.../frc/robot/lab/Challenges.java` | Each challenge's trial script and checks (with thresholds). |
| `.../frc/robot/lab/AKitLab.java` | Runs the scheduler and the trials, swaps challenges, and publishes everything below. |
| `.../frc/robot/lab/LabController.java` | The driver's `CommandXboxController`. Whatever it reads is logged as an input at `Lab/Driver`. |
| `.../frc/robot/lab/LabRecorder.java` | Saves a graded trial to its own log, as an AdvantageKit data receiver. |
| `.../frc/robot/lab/ReplayRunner.java` | Replays a log, in a second copy of the robot program. |
| `.../frc/robot/lab/ReplayLog.java` | Reads the log the replay wrote, and compares `RealOutputs` with `ReplayOutputs`. |
| `.../frc/robot/lab/LogAccess.java` | Reads AdvantageKit's log table, and clears what the last challenge left in it. |
| `.../src/main/deploy/match-logs/` | The logs that challenges 9 and 15 replay. See below for how they're made. |
| `src/challenges.ts` | Every challenge's text (idea, steps, the trial, code, hints, links) and its panels. |
| `src/lab.ts` | The NetworkTables topics shared with `AKitLab.java`, and the hooks that read them. |
| `src/coderunner/devMock.ts` | The in-memory robot for `bun run dev`. |

Adding or renaming a challenge means changing three places together: the class
in `challenges/`, its entry in `Challenges.java`, and its entry in
`src/challenges.ts` (same `id`).

## How replay works

Replays are real, not imitated:

1. `AKitLab` calls `LabRecorder.begin` when a trial starts. `LabRecorder` is a
   `LogDataReceiver`, so AdvantageKit hands it each loop's table, and it writes
   them to `build/akitlab/trial.wpilog` with a `WPILOGWriter`.
2. When the trial ends, `ReplayRunner` copies that log to a folder of its own, and
   starts a second JVM with the robot's own class path and JVM arguments, without
   `HALSIM_EXTENSIONS`, and with `AKIT_LOG_PATH` set. That's the variable
   `LogFileUtil.findReplayLog()` checks first. `Constants.currentMode` sees it too,
   so the second copy is in `Mode.REPLAY`, and takes that case in `Robot.java`.
3. `AKitLab` sees `Logger.hasReplaySource()`, marks `Hardware` as absent, makes the
   challenge named by `AKITLAB_CHALLENGE`, and from then on only reads
   `Lab/Driver` and runs the scheduler. The first copy of the program keeps running.
4. AdvantageKit exits the second JVM at the end of the log. `ReplayLog` reads its
   `_sim` log with WPILib's `DataLogReader`, and the replay checks are graded on it.

Three details that aren't obvious:

- **The first cycle of a log is special.** `Logger.start()` consumes it for the
  robot's constructor, so a replay never calls `robotPeriodic` for it. A trial's
  first loop has to run in both copies, so `LabRecorder` writes the loop before
  it first, with the student's values taken out. Without this, every replay is one
  loop behind.
- **AdvantageKit doesn't expect challenges to change while the program runs.**
  `LogAccess` clears the student's values, `@AutoLogOutput` registrations and
  dashboard inputs between runs, and `AKitLab` clears the scheduler's callbacks,
  all by reflection. Real robot code never needs this. An upgrade of AdvantageKit
  or WPILib could break it, so re-run the calibration after one.
- **Each replay has its own folder.** On some systems, an open log file can't be
  replaced. Older folders are deleted as far as possible.

A replay that takes longer than 45 seconds is stopped, and a watchdog in the second
JVM ends it if the first one has gone away.

## Topics

| Topic | Direction | Meaning |
| --- | --- | --- |
| `/SmartDashboard/AKitLab/RunRequest` | dashboard → robot | `"<id>\|trial#<nonce>"` or `"<id>\|free#<nonce>"` loads a challenge; `"#<nonce>"` stops. |
| `/SmartDashboard/AKitLab/Virtual` | dashboard → robot | The tab's buttons and keyboard: `[button bits, six axes]`, axes in `XboxController.Axis` order. `LabController.java` merges it with the Driver Station's joystick. |
| `/SmartDashboard/AKitLab/FieldEvent` | dashboard → robot | `"piece#<nonce>"` puts a piece in the feeder, in free play only. |
| `/Tuning/TargetRPM`, `/SmartDashboard/TargetRPM`, `/SmartDashboard/ShotPreset/selected` | both | The dashboard inputs of challenge 11. |
| `/AKitLab/Catalog` | robot → dashboard | JSON, published once at startup: each challenge's length, check labels, and whether it replays. |
| `/AKitLab/Status` | robot → dashboard | JSON, every loop: what's loaded, the phase, why it stopped, the buttons, and the true robot. `loops` changes every loop, so the dashboard uses it to spot a robot stuck in an endless loop. |
| `/AKitLab/Tree` | robot → dashboard | JSON, 10 times a second: every value the student has logged, with its kind, type and unit, and with poses decoded. |
| `/AKitLab/Result` | robot → dashboard | JSON, when a trial is graded: every check's outcome, the trace for the plots, and the replay's differences and series. |

The lab uses plain topics rather than `Logger.recordOutput`, so that the student's
log only holds what the student put there.

A trial starts when the robot is enabled in Teleop; disabling it or leaving Teleop
stops the trial. The trial's scripted driver replaces every other input. Trial time
counts robot loops, so the scripts and checks line up the same way every time.
Student code still sees real loop timing, which is why the checks leave some room.

## Reference solutions and calibration

`reference/solutions/` holds a solution for every challenge, and `reference/naive/`
holds attempts that make the mistake each challenge teaches. `reference/calibrate.sh`
runs them through the real program: `CalibrationTest.java` starts the real `Robot`,
asks for trials over NetworkTables just as the dashboard does, enables the robot,
and reads the graded results, with real replays in real child JVMs. It works in a
throwaway copy of the module, and needs `JAVA_HOME` set to a JDK 17 (WPILib's works).

```sh
reference/calibrate.sh scaffold   # only the example should pass
reference/calibrate.sh solved     # everything should pass, with every star
reference/calibrate.sh naive      # none of these should pass
reference/calibrate.sh solved first-output,mystery   # just these
GRADLE_ARGS=--offline reference/calibrate.sh solved  # with the dependencies cached
```

Use it after changing the simulation, a script or a threshold. The last line says
`ALL OK` or lists the problems. It's kept out of the module so students don't see
the solutions.

## The shipped match logs

Challenges 9 and 15 replay real logs, made by running this program with scripts
named `record-practice-match` and `record-qual-17` in `Challenges.java`, using the
unmodified scaffolds. They don't appear on the dashboard. To make them again:

```sh
reference/calibrate.sh record     # rewrites src/main/deploy/match-logs/
```

Do this if the inputs in those logs change: `Lab/Driver` (`DriverInputs.java`), or the
IO layers those challenges use (`subsystems/flywheel`, `subsystems/feeder` and
`challenges/mystery/ShooterIO.java`). Then run `solved` again.

The logs must stay out of any folder called `logs`: the module's `.gitignore`
ignores those, as WPILib's template does.

## Dev mock

On `bun run dev`, `src/coderunner/devMock.ts` stands in for `AKitLab.java` with a
flywheel that spins up while A is held, the keys each challenge expects, and a
result at the end, so the tab can be built and styled without a robot. Its checks
are placeholders; No Peeking shows a replay that doesn't match, to show the failed
view.

See [Custom Dashboards](https://mathewdunne.github.io/CodeRunner/lessons/custom-dashboards)
for the `window.coderunner` API.
