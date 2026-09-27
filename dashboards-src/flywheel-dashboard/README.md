# Flywheel Tuning dashboard (source)

Source for the **Flywheel Tuning** tab of the `flywheel-dashboard` lesson. It was
started from CodeRunner's `templates/dashboard-react` (React + Vite + TypeScript).

This folder is **not** part of the lesson module. Students get only the built
output, which lives in `modules/flywheel-dashboard/dashboard/`. Keep the source
out of the module, too: the module's Spotless config formats every `*.json` file
under it, including `package.json` and anything in `node_modules`.

## Work on it

```sh
bun install            # or: npm install
bun run dev            # sample data from src/coderunner/devMock.ts, no robot needed
```

## Rebuild the lesson copy

```sh
bun run build:lesson   # writes ../../modules/flywheel-dashboard/dashboard/
```

Commit the rebuilt `modules/flywheel-dashboard/dashboard/` together with any
source change. The tab is declared in
`modules/flywheel-dashboard/.coderunner/dashboards.json`.

## Topics

| Topic | Direction | Robot code |
| --- | --- | --- |
| `/SmartDashboard/Flywheel/TargetRPM`, `kP`, `kV` | dashboard → robot | `SmartDashboard.getNumber("Flywheel/...")` |
| `/AdvantageKit/RealOutputs/Flywheel/TargetRPM`, `VelocityRPM`, `Volts` | robot → dashboard | `Logger.recordOutput("Flywheel/...")` |
| `/AdvantageKit/RealOutputs/Flywheel/AtSpeed` | robot → dashboard | added by the student in step 4 |

See [Custom Dashboards](https://mathewdunne.github.io/CodeRunner/lessons/custom-dashboards)
for the `window.coderunner` API.
