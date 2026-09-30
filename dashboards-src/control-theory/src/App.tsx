import { useEffect, useMemo, useState } from "react";
import {
	CHALLENGES,
	type Challenge,
	findChallenge,
	type Level,
	optionLabel,
} from "./challenges";
import { useCodeRunnerContext, useNTValue, useRobotState } from "./coderunner/hooks";
import { ChallengeDetails } from "./components/ChallengeDetails";
import { Inline } from "./components/Inline";
import { MechanismView } from "./components/MechanismView";
import { Plot, type PlotData, type PlotLine } from "./components/Plot";
import { Results } from "./components/Results";
import { Tunables } from "./components/Tunables";
import {
	type Line,
	loadJson,
	type Sample,
	SANDBOX_GOAL,
	saveJson,
	type TrialResult,
	useLab,
} from "./lab";
import { MECHANISMS, type MechanismInfo } from "./mechanisms";

const SELECTED_KEY = "control-lab:selected";
const PROGRESS_KEY = "control-lab:progress";

type Progress = Record<string, { done: boolean; stars: number }>;

function groupByLevel(): [Level, Challenge[]][] {
	const groups = new Map<Level, Challenge[]>();
	for (const challenge of CHALLENGES) {
		const group = groups.get(challenge.level) ?? [];
		group.push(challenge);
		groups.set(challenge.level, group);
	}
	return [...groups];
}

const GROUPS = groupByLevel();

const COLORS = {
	goal: "var(--muted)",
	actual: "var(--accent)",
	sensor: "var(--muted)",
	estimate: "var(--orange)",
	extra: ["var(--purple)", "var(--teal)", "var(--pink)", "var(--good)", "var(--warn)", "var(--bad)"],
};

/** Where a mechanism sits before anything has run, in robot units. */
const RESTING: Record<string, number> = {
	flywheel: 0,
	steering: 0,
	elevator: 0,
	arm: (-45 * Math.PI) / 180,
};

/** Samples every plot line, converted to display units. */
function buildPlot(
	info: MechanismInfo,
	wraps: boolean,
	time: number[],
	goal: number[],
	output: number[],
	measured: number[],
	estimate: Line,
	plots: Record<string, Line>,
	volts: number[],
): PlotData {
	const show = (values: Line): Line => {
		const shown = values.map((v) => (v === null ? null : info.display(v)));
		if (!wraps) return shown;
		// A steering angle jumping from 180 to -180 isn't a real move: break the line.
		return shown.map((v, i) => {
			const previous = shown[i - 1];
			return v !== null && previous !== null && previous !== undefined && Math.abs(v - previous) > 180
				? null
				: v;
		});
	};
	const noisy = measured.some((m, i) => Math.abs(m - output[i]) > 1e-6);
	const lines: PlotLine[] = [{ label: "Goal", color: COLORS.goal, values: show(goal), dashed: true }];
	if (noisy) {
		lines.push({ label: "Sensor", color: COLORS.sensor, values: show(measured), faint: true });
	}
	lines.push({ label: "Actual", color: COLORS.actual, values: show(output) });
	if (estimate.some((v) => v !== null)) {
		lines.push({ label: "Your estimate", color: COLORS.estimate, values: show(estimate) });
	}
	Object.entries(plots).forEach(([name, values], i) => {
		lines.push({
			label: name,
			color: COLORS.extra[i % COLORS.extra.length],
			values: show(values),
		});
	});
	return { time, lines, volts };
}

function plotFromSamples(info: MechanismInfo, wraps: boolean, samples: Sample[]): PlotData {
	const names = new Set<string>();
	for (const s of samples) for (const name of Object.keys(s.plots)) names.add(name);
	const plots: Record<string, Line> = {};
	for (const name of names) plots[name] = samples.map((s) => s.plots[name] ?? null);
	return buildPlot(
		info,
		wraps,
		samples.map((s) => s.time),
		samples.map((s) => s.goal),
		samples.map((s) => s.output),
		samples.map((s) => s.measured),
		samples.map((s) => s.estimate),
		plots,
		samples.map((s) => s.volts),
	);
}

function plotFromResult(info: MechanismInfo, wraps: boolean, result: TrialResult): PlotData {
	const t = result.trace;
	return buildPlot(info, wraps, t.time, t.goal, t.output, t.measured, t.estimate, t.plots, t.volts);
}

export default function App() {
	const context = useCodeRunnerContext();
	const robot = useRobotState();
	const lab = useLab();

	const [selectedId, setSelectedId] = useState(() => {
		const id = loadJson<string>(SELECTED_KEY, "");
		return findChallenge(id) ? id : CHALLENGES[0].id;
	});
	const selected = findChallenge(selectedId) ?? CHALLENGES[0];
	const index = CHALLENGES.indexOf(selected);
	const info = MECHANISMS[selected.mechanism];
	const entry = lab.catalog?.find((c) => c.id === selected.id);
	const result = lab.results[selected.id];

	const [progress, setProgress] = useState<Progress>(() => loadJson(PROGRESS_KEY, {}));
	// The challenge and mode the live samples belong to.
	const [live, setLive] = useState({ id: "", mode: "" });
	const [sandboxGoal] = useNTValue<number>(SANDBOX_GOAL, 0);

	const select = (id: string) => {
		setSelectedId(id);
		saveJson(SELECTED_KEY, id);
	};

	useEffect(() => {
		if (lab.running !== "") setLive({ id: lab.running, mode: lab.mode });
	}, [lab.running, lab.mode]);

	// Record completed challenges and the most bonus stars earned.
	useEffect(() => {
		setProgress((previous) => {
			let changed = false;
			const next = { ...previous };
			for (const r of Object.values(lab.results)) {
				const old = next[r.id] ?? { done: false, stars: 0 };
				const merged = { done: old.done || r.passed, stars: Math.max(old.stars, r.stars) };
				if (merged.done !== old.done || merged.stars !== old.stars) {
					next[r.id] = merged;
					changed = true;
				}
			}
			if (changed) saveJson(PROGRESS_KEY, next);
			return changed ? next : previous;
		});
	}, [lab.results]);

	// Follow CodeRunner's light/dark theme.
	useEffect(() => {
		document.documentElement.dataset.theme = context?.theme ?? "dark";
	}, [context?.theme]);

	const selectedRunning = lab.running === selected.id;
	const sandbox = selectedRunning && lab.mode === "sandbox";
	const trialRunning = selectedRunning && lab.mode === "trial";
	const liveForSelected = live.id === selected.id && lab.samples.length > 0;
	// A finished trial shows its full graded trace; a stopped sandbox stays as it was.
	const liveSandbox = live.mode === "sandbox";
	const showLive = liveForSelected && (selectedRunning || liveSandbox);
	const wraps = selected.mechanism === "steering";

	const plot = useMemo<PlotData | null>(() => {
		if (showLive) return plotFromSamples(info, wraps, lab.samples);
		if (result) return plotFromResult(info, wraps, result);
		return null;
	}, [showLive, info, wraps, lab.samples, result]);

	const latest = liveForSelected ? lab.samples[lab.samples.length - 1] : undefined;
	const view = latest
		? { position: latest.position, velocity: latest.velocity, goal: latest.goal }
		: result && result.trace.time.length > 0
			? {
					position: result.trace.output[result.trace.output.length - 1],
					velocity: selected.mechanism === "flywheel" ? result.trace.output[result.trace.output.length - 1] : 0,
					goal: result.trace.goal[result.trace.goal.length - 1],
				}
			: { position: RESTING[selected.mechanism], velocity: 0, goal: null };

	const duration = entry?.duration ?? (result?.trace.time.at(-1) ?? 5);
	const lastTime = plot?.time.at(-1) ?? 0;
	const [plotStart, plotEnd] =
		showLive && liveSandbox ? [Math.max(lastTime - 6, 0), Math.max(lastTime, 6)] : [0, duration];

	const doneCount = CHALLENGES.filter((c) => progress[c.id]?.done).length;
	const starCount = CHALLENGES.reduce((sum, c) => sum + (progress[c.id]?.stars ?? 0), 0);
	const robotKnowsSelected = !lab.catalog || entry !== undefined;

	let plotOverlay: string | undefined;
	if (!lab.connected && !plot) {
		plotOverlay = "Waiting for the robot: click Start in the Driver Station";
	} else if (!plot) {
		plotOverlay = "Click Run trial to test your controller";
	}

	const pickerLabel = (challenge: Challenge) => {
		const p = progress[challenge.id];
		const mark = p?.done ? "✓ " : "";
		const stars = p?.stars ? ` ${"★".repeat(p.stars)}` : "";
		const running = challenge.id === lab.running ? "  (running)" : "";
		return `${mark}${optionLabel(challenge)}${stars}${running}`;
	};

	return (
		<main className="dashboard">
			<header className="status-bar">
				<span className={lab.connected ? "dot dot-on" : "dot"} />
				<span>
					{lab.connected ? "Robot program running" : "Waiting for robot: click Start in the Driver Station"}
				</span>
				<span className="progress-summary">
					{doneCount} of {CHALLENGES.length} complete
					{starCount > 0 && ` · ${starCount} ★`}
				</span>
				{robot && lab.connected && (
					<span className="robot-state">
						{robot.eStopped ? "E-stopped" : robot.enabled ? `Enabled · ${robot.mode}` : "Disabled (the lab runs anyway)"}
					</span>
				)}
			</header>

			<section className="card controls">
				<div className="picker">
					<button
						type="button"
						className="button icon-button"
						aria-label="Previous challenge"
						title="Previous challenge"
						disabled={index <= 0}
						onClick={() => select(CHALLENGES[index - 1].id)}
					>
						‹
					</button>
					<select aria-label="Challenge" value={selected.id} onChange={(event) => select(event.target.value)}>
						{GROUPS.map(([level, challenges]) => (
							<optgroup key={level} label={level}>
								{challenges.map((challenge) => (
									<option key={challenge.id} value={challenge.id}>
										{pickerLabel(challenge)}
									</option>
								))}
							</optgroup>
						))}
					</select>
					<button
						type="button"
						className="button icon-button"
						aria-label="Next challenge"
						title="Next challenge"
						disabled={index >= CHALLENGES.length - 1}
						onClick={() => select(CHALLENGES[index + 1].id)}
					>
						›
					</button>
				</div>
				<div className="actions">
					<button
						type="button"
						className="button button-primary"
						disabled={!lab.connected}
						title={lab.connected ? "Run the graded trial" : "Start the robot program in the Driver Station first"}
						onClick={() => lab.run(selected.id, "trial")}
					>
						▶ Run trial
					</button>
					<button
						type="button"
						className="button"
						disabled={!lab.connected}
						title="Run your controller freely: you set the goal and poke the mechanism"
						onClick={() => lab.run(selected.id, "sandbox")}
					>
						Sandbox
					</button>
					<button type="button" className="button" disabled={!lab.connected || lab.running === ""} onClick={lab.stop}>
						■ Stop
					</button>
					<span className="run-state">
						{lab.running
							? `${lab.mode === "sandbox" ? "Sandbox" : "Trial"}: ${optionLabel(findChallenge(lab.running) ?? selected)}`
							: lab.connected
								? "Nothing running"
								: ""}
					</span>
				</div>
			</section>

			{lab.error && (
				<div className="banner banner-error" role="alert">
					<strong>Your controller stopped with an error.</strong>
					<code className="error-text">{lab.error}</code>
					<span>
						Fix it in the file, click <strong>Restart</strong> in the Driver Station, and it will run again.
					</span>
				</div>
			)}
			{lab.stalled && (
				<div className="banner banner-warn" role="alert">
					<strong>The robot stopped responding.</strong>
					<span>
						Is there a loop that never ends? Fix it and click <strong>Restart</strong> in the Driver Station.
					</span>
				</div>
			)}
			{lab.connected && !robotKnowsSelected && (
				<div className="banner banner-warn">
					The robot program doesn't have this challenge. Is <code>Challenges.java</code> missing a line for it?
				</div>
			)}
			{entry?.problem && (
				<div className="banner banner-warn">
					<strong>This challenge's class couldn't be created when the robot started.</strong>
					<code className="error-text">{entry.problem}</code>
				</div>
			)}

			<section className="card lab">
				<div className="lab-grid">
					<div className="lab-view">
						<h2>
							{info.name}
							{sandbox && <span className="tag">Sandbox: click to set the goal</span>}
						</h2>
						<MechanismView
							kind={selected.mechanism}
							position={view.position}
							velocity={view.velocity}
							goal={view.goal}
							payload={liveForSelected && lab.payload}
							shots={liveForSelected ? lab.shots : 0}
							onPickGoal={sandbox ? lab.setSandboxGoal : undefined}
						/>
						{sandbox && (
							<div className="sandbox">
								<label className="sandbox-goal">
									<span>
										Goal <strong>{info.display(sandboxGoal).toFixed(info.decimals)}</strong> {info.unit}
									</span>
									<input
										type="range"
										min={info.goalMin}
										max={info.goalMax}
										step={info.goalStep}
										value={info.display(sandboxGoal)}
										onChange={(event) => lab.setSandboxGoal(info.robot(Number(event.target.value)))}
									/>
								</label>
								<div className="sandbox-events">
									{info.events.map((event) => (
										<button key={event.id} type="button" className="button" onClick={() => lab.sandboxEvent(event.id)}>
											{event.label}
										</button>
									))}
								</div>
							</div>
						)}
					</div>
					<div className="lab-tune">
						<h2>Tune</h2>
						<Tunables
							challengeId={selected.id}
							tunables={entry?.tunables}
							sliders={selected.sliders}
							connected={lab.connected}
						/>
					</div>
				</div>
				<div>
					<h2>
						{info.quantity} and voltage
						{showLive && trialRunning && <span className="tag">live</span>}
						{!showLive && result && <span className="tag tag-plain">last trial</span>}
					</h2>
					<Plot
						data={plot ?? { time: [], lines: [], volts: [] }}
						start={plotStart}
						end={plotEnd}
						unit={info.unit}
						range={[info.display(RESTING[selected.mechanism]), info.display(RESTING[selected.mechanism])]}
						decimals={info.decimals}
						overlay={plotOverlay}
					/>
				</div>
			</section>

			<Results entry={entry} result={result} running={trialRunning} />

			<ChallengeDetails key={selected.id} challenge={selected} />

			<details className="card how">
				<summary>How the Control Lab works</summary>
				<ol className="steps">
					<li>
						<Inline text="Every challenge is a class in `challenges/` that `implements Controller`. Its `calculate` method runs every 20 ms: read the mechanism's sensors and goal, and return a voltage." />
					</li>
					<li>
						<Inline text="Save, then click **Restart** in the Driver Station: Java code has to be rebuilt before the robot can use it. The last thing you ran starts again by itself." />
					</li>
					<li>
						<Inline text="**Run trial** runs a scripted test (goal changes, shots, surprise payloads) and grades it. Complete every check to finish the challenge; bonus checks earn stars." />
					</li>
					<li>
						<Inline text="**Sandbox** runs your controller with no script: drag the goal slider or click the mechanism, and poke it with the buttons." />
					</li>
					<li>
						<Inline text="Each `TunableNumber` in your class gets a slider. Slider values are remembered, even after a restart. Once you like them, copy them into your code as the defaults." />
					</li>
					<li>
						<Inline text={'Draw your own lines on the plot with `mechanism.plot("name", value)`. Everything is also logged under `ControlLab/` for AdvantageScope.'} />
					</li>
				</ol>
			</details>
		</main>
	);
}
