# Control Lab dashboard (source)

Source for the **Control Lab** tab of the `control-theory` lesson. It was
started from CodeRunner's `templates/dashboard-react` (React + Vite + TypeScript),
like the LED Challenges dashboard.

This folder is **not** part of the lesson module. Students get only the built
output, which lives in `modules/control-theory/dashboard/`.

## Work on it

```sh
bun install            # or: npm install
bun run dev            # no robot needed: see "Dev mock" below
```

## Rebuild the lesson copy

```sh
bun run build:lesson   # writes ../../modules/control-theory/dashboard/
```

Commit the rebuilt `modules/control-theory/dashboard/` together with any source
change. The tab is declared in `modules/control-theory/.coderunner/dashboards.json`.

## How the lesson is split

The robot program owns the lab: the simulated mechanisms, each trial's script
(goal changes, shots, payloads, sensor noise) and how it's graded. The
dashboard owns what the student reads.

| Where | What it holds |
| --- | --- |
| `modules/control-theory/src/main/java/frc/robot/challenges/` | The student scaffolds, one `Controller` class per challenge. |
| `modules/control-theory/src/main/java/frc/robot/lab/Challenges.java` | Each challenge's mechanism, trial script and checks (with thresholds). |
| `modules/control-theory/src/main/java/frc/robot/lab/Plant.java` | The mechanism models (`volts = kS·sign(v) + kG·g(x) + kV·v + kA·a`) and their constants. |
| `src/challenges.ts` | Every challenge's text (idea, steps, code, hints, book sections, links) and slider ranges. |
| `src/mechanisms.ts` | Display units and sandbox ranges per mechanism. |
| `src/lab.ts` | The NetworkTables topics shared with `ControlLab.java`, and the hooks that read them. |
| `src/coderunner/devMock.ts` | The in-memory robot for `bun run dev`. |

Adding or renaming a challenge means changing three places together: the class
in `challenges/`, its entry in `Challenges.java`, and its entry in
`src/challenges.ts` (same `id`, and `className` matching the Java file).

## Topics

| Topic | Direction | Meaning |
| --- | --- | --- |
| `/SmartDashboard/ControlLab/RunRequest` | dashboard → robot | `"<id>\|trial#<nonce>"` or `"<id>\|sandbox#<nonce>"` runs a challenge; `"#<nonce>"` stops. |
| `/SmartDashboard/ControlLab/Sandbox/Goal` | both | Sandbox goal, in robot units. The robot resets it when a sandbox starts. |
| `/SmartDashboard/ControlLab/Sandbox/Event` | dashboard → robot | `"shot#<nonce>"`, `"payload#<nonce>"` (toggles) or `"bump#<nonce>"`. |
| `/SmartDashboard/ControlLab/Tune/<id>/<name>` | both | One per `TunableNumber`. The dashboard remembers values in browser storage and re-sends them when the robot program restarts. |
| `/ControlLab/Catalog` | robot → dashboard | JSON, published once at startup: each challenge's mechanism, check labels and tunable numbers. |
| `/ControlLab/Result` | robot → dashboard | JSON, when a trial ends: every check's outcome, the SysId fit (challenge 10), and the full trace for the plot. |
| `/AdvantageKit/RealOutputs/ControlLab/Sample` | robot → dashboard | `[time, goal, output, measured, volts, estimate, position, velocity, …plot values]`, every loop. |
| `/AdvantageKit/RealOutputs/ControlLab/PlotNames` | robot → dashboard | Names of the student's `mechanism.plot(…)` lines, in the order they follow in `Sample`. |
| `/AdvantageKit/RealOutputs/ControlLab/{Running,Mode,RunId,Error,Time,Payload,Shots}` | robot → dashboard | What's running, why it stopped, and what to animate. `Time` changes every loop, so the dashboard uses it to spot a robot stuck in an endless loop. |

Trial time counts robot loops, not the wall clock, and sensor noise uses a fixed
seed, so a trial gives the same grade every time for the same code and gains.

## Reference solutions and calibration

`reference/CalibrationTest.java` holds a solved controller for every challenge,
plus deliberately naive ones (P without feedforward, PID without an I-zone, a
slow-ramp-only SysId test, …), and runs them all through the real trial and
grading code. Use it after changing a plant, a script or a threshold, to check
that every challenge can still be passed and that the checks still catch the
mistake the challenge teaches.

It is kept out of the module so students don't see the solutions. To run it:

```sh
mkdir -p ../../modules/control-theory/src/test/java/frc/robot/lab
cp reference/CalibrationTest.java ../../modules/control-theory/src/test/java/frc/robot/lab/
cd ../../modules/control-theory
./gradlew test -i | grep -E "^ *(OK|NOPE|PASS|FAIL)"     # SWEEP=1/2/3 run the parameter sweeps instead
rm -r src/test
```

Every "reference solutions" line should say `OK`; the naive attempts should say
`NOPE`, except the few that are also valid answers.

## Dev mock

On `bun run dev`, `src/coderunner/devMock.ts` stands in for `ControlLab.java`
with a rough copy of the mechanisms driven by simple solved controllers, so the
tab can be built and styled without a robot. Its checks are placeholders;
Proportional Control fails one on purpose, to show a failed result.

See [Custom Dashboards](https://mathewdunne.github.io/CodeRunner/lessons/custom-dashboards)
for the `window.coderunner` API.
