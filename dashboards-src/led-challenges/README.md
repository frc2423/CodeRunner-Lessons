# LED Challenges dashboard (source)

Source for the **LED Challenges** tab of the `led-challenges` lesson. It was
started from CodeRunner's `templates/dashboard-react` (React + Vite + TypeScript),
like the flywheel dashboard.

This folder is **not** part of the lesson module. Students get only the built
output, which lives in `modules/led-challenges/dashboard/`. Keep the source out
of the module: the module's Spotless config formats every `*.json` file under it,
including `package.json` and anything in `node_modules`.

## Work on it

```sh
bun install            # or: npm install
bun run dev            # no robot needed: see "Dev mock" below
```

## Rebuild the lesson copy

```sh
bun run build:lesson   # writes ../../modules/led-challenges/dashboard/
```

Commit the rebuilt `modules/led-challenges/dashboard/` together with any source
change. The tab is declared in `modules/led-challenges/.coderunner/dashboards.json`.

## Where things live

| File | What it holds |
| --- | --- |
| `src/challenges.ts` | Every challenge's text (steps, code, hints, links), its goal animation, and its automatic checks. |
| `src/leds.ts` | A JavaScript copy of `LedStrip.java` that the goal animations draw on. |
| `src/robot.ts` | The NetworkTables topics shared with `LedSubsystem.java`, and the hooks that read them. |
| `src/coderunner/devMock.ts` | The in-memory robot for `bun run dev`. |

Adding or renaming a challenge means changing three places together: the class
in `modules/led-challenges/src/main/java/frc/robot/subsystems/LEDS/`, its line in
`LedChallenges.java`, and its entry in `src/challenges.ts` (same `id`, and
`className` matching the Java file).

## Topics

| Topic | Direction | Robot code |
| --- | --- | --- |
| `/SmartDashboard/LEDs/RunRequest` | dashboard → robot | `"<challenge id>#<nonce>"` runs a challenge, `"#<nonce>"` stops. The nonce makes every click a new value. |
| `/AdvantageKit/RealOutputs/LEDs/Colors` | robot → dashboard | `int[]`, one `0xRRGGBB` per LED |
| `/AdvantageKit/RealOutputs/LEDs/Running` | robot → dashboard | id of the running challenge, or `""` |
| `/AdvantageKit/RealOutputs/LEDs/Seconds` | robot → dashboard | seconds since Run; changes every loop, so the dashboard uses it to spot a robot stuck in an endless loop |
| `/AdvantageKit/RealOutputs/LEDs/Error` | robot → dashboard | why the last challenge stopped, with the student's file and line, or `""` |
| `/AdvantageKit/RealOutputs/LEDs/Challenges` | robot → dashboard | every challenge id the robot program knows |

The LED data travels over NetworkTables rather than HALSim because a dashboard
can only reach the robot through `window.coderunner.nt`. The robot program still
drives a real `AddressableLED` on PWM 0, so the same code works on a roboRIO.

After a robot restart (the student edited their code), the dashboard sends the
last run request again, so the challenge they were working on starts by itself.

## Dev mock

On `bun run dev`, `src/coderunner/devMock.ts` stands in for `LedSubsystem.java`
and plays each challenge's goal as if it were solved, so **Your robot** matches
**Goal**. Running **Stripes** reports a made-up exception instead, to show the
error banner.

See [Custom Dashboards](https://mathewdunne.github.io/CodeRunner/lessons/custom-dashboards)
for the `window.coderunner` API.
