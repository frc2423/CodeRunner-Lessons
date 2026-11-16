# Smart Steering Playground dashboard (source)

Source for the **Playground** tab of the `smart-steering-playground` lesson.
It was started from CodeRunner's `templates/dashboard-react` (React + Vite +
TypeScript), like the other lesson dashboards.

This folder is **not** part of the lesson module. Students get only the built
output, which lives in `modules/smart-steering-playground/dashboard/`.

## Work on it

```sh
bun install            # or: npm install
bun run dev            # no robot needed: see "Dev mock" below
```

## Rebuild the lesson copy

```sh
bun run build:lesson   # writes ../../modules/smart-steering-playground/dashboard/
```

Commit the rebuilt `modules/smart-steering-playground/dashboard/` together
with any source change. The tab is declared in
`modules/smart-steering-playground/.coderunner/dashboards.json`.

## How the lesson is split

The robot program owns the simulation: the robot, its physics and bumps, the
obstacles from code, and calling the student's `SmartSteering.steer`. The
dashboard owns the drawn obstacles (kept in the browser's local storage and
sent to the robot), the keyboard, and everything the student sees.

| Where | What it holds |
| --- | --- |
| `modules/smart-steering-playground/src/main/java/frc/robot/SmartSteering.java` | The student scaffold: `steer(requested, robotPose, currentSpeeds, obstacles)`. |
| `.../frc/robot/Obstacles.java` | The obstacles written in code (students edit it too). |
| `.../frc/robot/playground/Playground.java` | The loop: reads the sticks and the tab, calls `steer`, steps the world, publishes. |
| `.../frc/robot/playground/World.java` | The physics: acceleration limits, pushing the robot out of obstacles, counting bumps. |
| `.../frc/robot/playground/Geometry.java` | Rectangle overlap/separation/distance (separating axis test); public for students. |
| `.../frc/robot/playground/Debug.java` | Lets student code draw on the tab's field and show values. |
| `.../frc/robot/playground/DriverInput.java` | Sticks → field-relative `ChassisSpeeds`, from the Driver Station's port 0 or the tab's keyboard. |
| `src/playground.ts` | The NetworkTables topics shared with `Playground.java`, and the hook that reads them. |
| `src/components/FieldView.tsx` | The field, drawing and moving obstacles. |
| `src/coderunner/devMock.ts` | The in-memory robot for `bun run dev`. |

## Topics

| Topic | Direction | Meaning |
| --- | --- | --- |
| `/SmartDashboard/Playground/Keys` | dashboard → robot | The tab's keyboard as six axes in `XboxController.Axis` order (same keys as the Driver Station's keyboard mode). A non-zero axis wins over the Driver Station's joystick. |
| `/SmartDashboard/Playground/DrawnObstacles` | dashboard → robot | Drawn obstacles, five numbers each: center X, center Y, X width, Y width, rotation (rad). Re-sent whenever the robot program (re)connects. |
| `/SmartDashboard/Playground/SmartSteering` | dashboard → robot | Whether to call `SmartSteering.steer` at all. |
| `/SmartDashboard/Playground/Command` | dashboard → robot | `"reset#<nonce>"` puts the robot back at the start and clears the bumps. |
| `/Playground/Config` | robot → dashboard | JSON, published once: field and robot size, max speeds, start pose (from `PlaygroundConstants.java`). |
| `/AdvantageKit/RealOutputs/Playground/State` | robot → dashboard | The robot packed in one array; see `toState` in `src/playground.ts` and `publish()` in `Playground.java`. |
| `.../Playground/Obstacles` | robot → dashboard | Every obstacle except the walls, six numbers each (the five above, then 0 for code or 1 for drawn). |
| `.../Playground/Bumps` | robot → dashboard | The last 12 bumps: `[x, y, time, speed]` each. |
| `.../Playground/Debug/{Shapes,Values}` | robot → dashboard | What student code drew this loop (7 numbers per shape: type, color, then up to five numbers), and `"name\tvalue"` lines. |
| `.../Playground/{Error,ObstacleError,Time}` | robot → dashboard | Why `steer` or `Obstacles.create` failed, and the robot time (to spot a robot stuck in an endless loop). |
| `.../Playground/{RobotPose,ModuleStates,RequestedSpeeds,SteeredSpeeds}` | robot → AdvantageScope | For studying a run in AdvantageScope. |

The robot only drives while it's enabled in Teleop. `steer` is called every
loop anyway (with zero requested speeds while disabled), so debug drawings show
up before the robot moves.

## Reference solution and calibration

`reference/ReferenceSmartSteering.java` is a solution (slide along obstacles
with a stopping-distance speed cap, brake first when the robot is already too
fast, and don't spin a corner into anything). `reference/SteeringCalibrationTest.java`
checks the geometry and physics, then drives scripted runs and 20 minutes of
random driving with no steering, the student scaffold, and the reference, and
compares bumps. Use it after changing the physics, the robot or the default
obstacles.

It is kept out of the module so students don't see the solution. To run it:

```sh
mkdir -p ../../modules/smart-steering-playground/src/test/java/frc/robot/playground
cp reference/*.java ../../modules/smart-steering-playground/src/test/java/frc/robot/playground/
cd ../../modules/smart-steering-playground
./gradlew test -i | grep -E "bumps|FAILED|PASSED"
rm -r src/test
```

The reference should have no bumps in every scripted run, and avoid at least
98% of the random-driving bumps (it still grazes things now and then when
squeezed between two obstacles at once).

## Dev mock

On `bun run dev`, `src/coderunner/devMock.ts` stands in for `Playground.java`
with the same physics, the default obstacles and the starting (do-nothing)
`SmartSteering`. Drive it with the keyboard. In the browser console,
`window.mockDisabled = true` pretends the robot is disabled and
`window.mockError = "..."` pretends the steering code crashed.

See [Custom Dashboards](https://mathewdunne.github.io/CodeRunner/lessons/custom-dashboards)
for the `window.coderunner` API.
