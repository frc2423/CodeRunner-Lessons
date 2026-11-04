import { useEffect, useState } from "react";
import {
	type ButtonRef,
	CHALLENGES,
	type Challenge,
	findChallenge,
	type Level,
	optionLabel,
	type PlotSpec,
} from "./challenges";
import { useCodeRunnerContext, useRobotState } from "./coderunner/hooks";
import { Brief } from "./components/Brief";
import { ChallengeDetails } from "./components/ChallengeDetails";
import { CommandTimeline } from "./components/CommandTimeline";
import { Controls } from "./components/Controls";
import { FieldView } from "./components/FieldView";
import { Inline } from "./components/Inline";
import { LogView } from "./components/LogView";
import { ReplayPanel } from "./components/ReplayPanel";
import { StepPlot } from "./components/StepPlot";
import { TuningPanel } from "./components/TuningPanel";
import { loadJson, type Points, saveJson, useLab } from "./lab";

const SELECTED_KEY = "akit-lab:selected";
const PROGRESS_KEY = "akit-lab:progress";

/** A stable empty list, so the driver panel doesn't start over on every render. */
const NO_BUTTONS: ButtonRef[] = [];

/** How much time free play's plots show. */
const FREE_PLAY_WINDOW = 15;

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

function Plots({
	specs,
	trace,
	start,
	end,
}: {
	specs: PlotSpec[];
	trace: Record<string, Points>;
	start: number;
	end: number;
}) {
	return (
		<section className="card panel">
			{specs.map((spec) => (
				<StepPlot
					key={spec.title}
					title={spec.title}
					unit={spec.unit}
					range={spec.range}
					start={start}
					end={end}
					lines={spec.lines.map((line) => ({ ...line, points: trace[line.id] ?? [] }))}
				/>
			))}
		</section>
	);
}

export default function App() {
	const context = useCodeRunnerContext();
	const robot = useRobotState();
	const lab = useLab();
	const { status } = lab;

	const [selectedId, setSelectedId] = useState(() => {
		const id = loadJson<string>(SELECTED_KEY, "");
		return findChallenge(id) ? id : CHALLENGES[0].id;
	});
	const selected = findChallenge(selectedId) ?? CHALLENGES[0];
	const index = CHALLENGES.indexOf(selected);
	const entry = lab.catalog?.find((c) => c.id === selected.id);
	const result = lab.results[selected.id];
	const [progress, setProgress] = useState<Progress>(() => loadJson(PROGRESS_KEY, {}));

	const select = (id: string) => {
		setSelectedId(id);
		saveJson(SELECTED_KEY, id);
	};

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

	const running = status?.running ?? "";
	const loaded = running !== "" ? findChallenge(running) : undefined;
	const selectedLoaded = running === selected.id;
	const phase = selectedLoaded ? (status?.phase ?? "") : "";
	const trialActive =
		selectedLoaded &&
		status?.mode === "trial" &&
		(phase === "waiting" || phase === "running" || phase === "replaying");
	const trialRunning = selectedLoaded && status?.mode === "trial" && phase === "running";
	const replaying = phase === "replaying";
	const freePlay = selectedLoaded && status?.mode === "free";
	const logOnly = entry?.logOnly ?? false;
	const robotEnabled = robot?.enabled ?? false;
	const now = status?.time ?? 0;

	const doneCount = CHALLENGES.filter((c) => progress[c.id]?.done).length;
	const starCount = CHALLENGES.reduce((sum, c) => sum + (progress[c.id]?.stars ?? 0), 0);
	const robotKnowsSelected = !lab.catalog || entry !== undefined;

	let message = "";
	if (phase === "waiting") message = "The trial starts when you **Enable** the robot in **Teleop**, in the Driver Station.";
	else if (replaying) message = "Replaying: running your code again, from the log…";
	else if (freePlay && !robotEnabled) {
		message = "Free play is loaded, but commands only run while the robot is enabled. **Enable** it in **Teleop**, in the Driver Station.";
	}

	const pickerLabel = (challenge: Challenge) => {
		const p = progress[challenge.id];
		const mark = p?.done ? "✓ " : "";
		const stars = p?.stars ? ` ${"★".repeat(p.stars)}` : "";
		return `${mark}${optionLabel(challenge)}${stars}${challenge.id === running ? "  (loaded)" : ""}`;
	};

	let runState = lab.connected ? "Nothing loaded" : "Click Start in the Driver Station";
	if (loaded) {
		const what =
			status?.mode === "free"
				? status.phase === "done"
					? "Trial done · free play"
					: "Free play"
				: status?.phase === "replaying"
					? "Replaying"
					: "Trial";
		runState = selectedLoaded ? what : `${what}: ${optionLabel(loaded)}`;
	}
	const robotMode = !robot
		? ""
		: robot.eStopped
			? "E-stopped"
			: robot.enabled
				? `Enabled · ${robot.mode === "auto" ? "Auto" : robot.mode === "test" ? "Test" : "Teleop"}`
				: "Disabled";

	// While the trial runs, or in free play, plots follow the live log. Afterwards they hold the whole trial.
	const freePlayLive = freePlay && phase !== "done";
	const live = trialRunning || freePlayLive;
	const showTrace = !live && result ? result.trace : selectedLoaded ? lab.history : {};
	const plotEnd = live
		? trialRunning
			? (status?.duration ?? 5)
			: Math.max(now, FREE_PLAY_WINDOW)
		: entry?.duration || status?.duration || 5;
	const plotStart = freePlayLive ? Math.max(now - FREE_PLAY_WINDOW, 0) : 0;

	const { panels } = selected;
	const sticks = panels.field ?? false;
	const hasControls = (panels.buttons?.length ?? 0) > 0 || sticks;
	const canPress = freePlay && robotEnabled;
	let controlsNote = "";
	if (trialRunning) controlsNote = "The trial's scripted driver is at the controls. Watch it press the buttons.";
	else if (!freePlay) controlsNote = "Click Free play to press these yourself.";
	else if (!robotEnabled) controlsNote = "Enable the robot to press these.";

	return (
		<main className="dashboard">
			<header className="toolbar">
				<span
					className={lab.connected ? "dot dot-on" : "dot"}
					title={lab.connected ? "Robot program running" : "Waiting for the robot program"}
				/>
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
						disabled={!lab.connected || replaying}
						title={lab.connected ? "" : "Start the robot program in the Driver Station first"}
						onClick={() => lab.run(selected.id, "trial")}
					>
						▶ {logOnly ? "Replay the match" : "Run trial"}
					</button>
					{!logOnly && (
						<button
							type="button"
							className="button"
							disabled={!lab.connected}
							title="Load this challenge's code and press the buttons yourself"
							onClick={() => lab.run(selected.id, "free")}
						>
							Free play
						</button>
					)}
					<button
						type="button"
						className="button icon-button"
						aria-label="Stop"
						title="Stop"
						disabled={!lab.connected || running === ""}
						onClick={lab.stop}
					>
						■
					</button>
				</div>
				<span className="run-state">
					{runState}
					{robotMode && ` · ${robotMode}`}
					<span className="progress-summary" title="Challenges complete">
						{doneCount}/{CHALLENGES.length}
						{starCount > 0 && ` · ${starCount}★`}
					</span>
				</span>
			</header>

			{loaded && !selectedLoaded && (
				<p className="muted loaded-note">
					The robot is running <strong>{optionLabel(loaded)}</strong>.{" "}
					<button type="button" className="link-button" onClick={() => select(loaded.id)}>
						Show it
					</button>
				</p>
			)}
			{status?.error && (
				<div className="banner banner-error" role="alert">
					<span>
						<strong>Your code stopped with an error.</strong> Fix it and click <strong>Restart</strong> in the Driver
						Station.
					</span>
					<code className="error-text">{status.error}</code>
				</div>
			)}
			{lab.stalled && (
				<div className="banner banner-warn" role="alert">
					<strong>The robot stopped responding.</strong> Is there a loop that never ends? Fix it and click{" "}
					<strong>Restart</strong>.
				</div>
			)}
			{lab.connected && !robotKnowsSelected && (
				<div className="banner banner-warn">
					The robot program doesn't have this challenge. Is <code>Challenges.java</code> missing a line for it?
				</div>
			)}
			{status?.notice && (
				<div className="banner banner-warn" role="status">
					{status.notice}
				</div>
			)}

			<Brief
				challenge={selected}
				entry={entry}
				result={result}
				trialActive={trialActive}
				clock={trialRunning && status ? { now, duration: status.duration } : null}
				message={message}
			/>

			{hasControls && !logOnly && (
				<Controls
					buttons={panels.buttons ?? NO_BUTTONS}
					sticks={sticks}
					piece={panels.piece ?? false}
					bits={selectedLoaded ? (status?.buttons ?? 0) : 0}
					active={canPress}
					note={controlsNote}
					hasPiece={selectedLoaded && (status?.hasPiece ?? false)}
					onChange={lab.setVirtual}
					onPiece={lab.loadPiece}
				/>
			)}
			{panels.tuning && <TuningPanel active={canPress} onTuning={lab.setTuning} onPreset={lab.setPreset} />}
			{panels.field && <FieldView tree={selectedLoaded ? lab.tree : undefined} />}
			{panels.commands && <CommandTimeline trace={showTrace} end={plotEnd} />}
			{panels.plots && panels.plots.length > 0 && (
				<Plots specs={panels.plots} trace={showTrace} start={plotStart} end={plotEnd} />
			)}
			{!logOnly && <LogView panels={panels} tree={selectedLoaded ? lab.tree : undefined} />}
			{entry?.replays && <ReplayPanel challenge={selected} result={result} replaying={replaying} logOnly={logOnly} />}

			<ChallengeDetails key={selected.id} challenge={selected} />

			<details className="card how">
				<summary>How the AdvantageKit Lab works</summary>
				<ol className="steps">
					<li>
						<Inline text="Each challenge is a class in `challenges/` that extends `LabChallenge`: a small RobotContainer, with the driver's `controller`. Some challenges have a folder of their own too. Everything runs on a simulated robot through a class called `Hardware`, which stands in for your motor controller and sensor libraries." />
					</li>
					<li>
						<Inline text="Save, then click **Restart** in the Driver Station: Java code has to be rebuilt before the robot can use it. The last thing you ran loads again by itself." />
					</li>
					<li>
						<Inline text="**Run trial** loads your code and waits for you to **Enable** the robot in **Teleop**. Then a scripted driver presses the buttons, and the trial is graded. You don't need a controller." />
					</li>
					<li>
						<Inline text="From challenge 5 on, each trial saves its log, and then **really replays it**: the lab starts a second copy of your program in AdvantageKit's `REPLAY` mode, reading that log. It writes a new log, the lab compares the two runs, and you see the result here. The logs of the last replay are in the `logs/` folder, as `last-run.wpilog` and `last-run_sim.wpilog`." />
					</li>
					<li>
						<Inline text="**Free play** loads your code and lets you press the buttons, once you **Enable** the robot. Click them here, or click this tab and use the keys shown on them." />
					</li>
					<li>
						<Inline text="While the program runs, everything in your log is also in the **AdvantageScope** tab: outputs under `AdvantageKit/RealOutputs/`, and inputs under `AdvantageKit/`." />
					</li>
				</ol>
			</details>
		</main>
	);
}
