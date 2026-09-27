import { useEffect } from "react";
import {
	useCodeRunnerContext,
	useNTConnection,
	useNTValue,
	useRobotState,
} from "./coderunner/hooks";
import { HistoryChart } from "./components/HistoryChart";
import { NumberSetting } from "./components/NumberSetting";

// Values RobotContainer.java logs with Logger.recordOutput("Flywheel/...").
const OUTPUTS = "/AdvantageKit/RealOutputs/Flywheel";
// Values RobotContainer.java reads with SmartDashboard.getNumber("Flywheel/...").
const INPUTS = "/SmartDashboard/Flywheel";

const MAX_RPM = 6000;

const CHART_SERIES = [
	{ label: "Target", topic: `${OUTPUTS}/TargetRPM`, color: "#f5b942" },
	{ label: "Velocity", topic: `${OUTPUTS}/VelocityRPM`, color: "#4f8cff" },
];

function clampFraction(value: number | undefined): number {
	return value === undefined ? 0 : Math.min(Math.max(value / MAX_RPM, 0), 1);
}

export default function App() {
	const context = useCodeRunnerContext();
	const connected = useNTConnection();
	const robot = useRobotState();

	const [velocity] = useNTValue<number>(`${OUTPUTS}/VelocityRPM`);
	const [target] = useNTValue<number>(`${OUTPUTS}/TargetRPM`);
	const [volts] = useNTValue<number>(`${OUTPUTS}/Volts`);
	const [atSpeed] = useNTValue<boolean>(`${OUTPUTS}/AtSpeed`);

	// Follow CodeRunner's light/dark theme.
	useEffect(() => {
		document.documentElement.dataset.theme = context?.theme ?? "dark";
	}, [context?.theme]);

	return (
		<main className="dashboard">
			<header className="status-bar">
				<span className={connected ? "dot dot-on" : "dot"} />
				<span>
					{connected
						? "Connected to robot"
						: "Waiting for robot — click Start in the Driver Station"}
				</span>
				{robot && (
					<span className="robot-state">
						{robot.eStopped
							? "E-stopped"
							: robot.enabled
								? `Enabled · ${robot.mode}`
								: "Disabled — enable to spin the flywheel"}
					</span>
				)}
			</header>

			<section className="cards">
				<article className="card">
					<h2>Flywheel speed</h2>
					<p className="big-number">
						{velocity !== undefined ? velocity.toFixed(0) : "—"}
						<small> RPM</small>
					</p>
					<div className="meter" aria-hidden="true">
						<div
							className="meter-fill"
							style={{ width: `${clampFraction(velocity) * 100}%` }}
						/>
						<div
							className="meter-target"
							style={{ left: `${clampFraction(target) * 100}%` }}
						/>
					</div>
					<dl className="stats">
						<dt>Target</dt>
						<dd>{target !== undefined ? `${target.toFixed(0)} RPM` : "—"}</dd>
						<dt>Error</dt>
						<dd>
							{target !== undefined && velocity !== undefined
								? `${(target - velocity).toFixed(0)} RPM`
								: "—"}
						</dd>
						<dt>Voltage</dt>
						<dd>{volts !== undefined ? `${volts.toFixed(2)} V` : "—"}</dd>
					</dl>
				</article>

				<article className="card">
					<h2>At speed</h2>
					{atSpeed === undefined ? (
						<p className="muted">
							Not published yet. Lesson step 4: log{" "}
							<code>Flywheel/AtSpeed</code>.
						</p>
					) : (
						<p className={atSpeed ? "badge badge-good" : "badge badge-bad"}>
							{atSpeed ? "At speed" : "Not at speed"}
						</p>
					)}
				</article>

				<article className="card">
					<h2>Tuning</h2>
					<NumberSetting
						topic={`${INPUTS}/TargetRPM`}
						label="Target RPM"
						min={0}
						max={MAX_RPM}
						step={100}
						defaultValue={3000}
						digits={0}
					/>
					<NumberSetting
						topic={`${INPUTS}/kP`}
						label="kP (volts per RPM of error)"
						min={0}
						max={0.02}
						step={0.0005}
						defaultValue={0.002}
						digits={4}
					/>
					<NumberSetting
						topic={`${INPUTS}/kV`}
						label="kV (volts per RPM of target)"
						min={0}
						max={0.004}
						step={0.0001}
						defaultValue={0}
						digits={4}
					/>
				</article>
			</section>

			<article className="card">
				<h2>Speed history</h2>
				<HistoryChart series={CHART_SERIES} max={MAX_RPM} />
			</article>
		</main>
	);
}
