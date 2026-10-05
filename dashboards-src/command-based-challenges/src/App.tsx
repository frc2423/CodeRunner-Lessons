import { useEffect, useState } from "react";
import { CHALLENGES, type Challenge, findChallenge, type Level, optionLabel } from "./challenges";
import { useCodeRunnerContext, useRobotState } from "./coderunner/hooks";
import { ChallengeDetails } from "./components/ChallengeDetails";
import { FieldView } from "./components/FieldView";
import { Gamepad } from "./components/Gamepad";
import { Inline } from "./components/Inline";
import { Results } from "./components/Results";
import { RobotView } from "./components/RobotView";
import { Timeline } from "./components/Timeline";
import { FIELD } from "./field";
import { loadJson, saveJson, useLab } from "./lab";

const SELECTED_KEY = "command-lab:selected";
const PROGRESS_KEY = "command-lab:progress";

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

/** Where the robot starts in each challenge (the rest start at the shooting spot). */
const STARTS: Record<string, { x: number; y: number; heading: number }> = {
	"drive-with-sticks": { x: 2.0, y: 1.5, heading: 0 },
	climber: { x: FIELD.tower.x, y: FIELD.tower.y, heading: 0 },
	"aim-at-hub": { x: 2.5, y: 2.0, heading: Math.PI },
};
const SPOT = { x: FIELD.shootingSpot.x, y: FIELD.shootingSpot.y, heading: 0 };

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

	const loaded = lab.running !== "" ? findChallenge(lab.running) : undefined;
	const selectedLoaded = lab.running === selected.id;
	const trialActive = selectedLoaded && lab.mode === "trial" && (lab.phase === "waiting" || lab.phase === "running");
	const trialRunning = selectedLoaded && lab.mode === "trial" && lab.phase === "running";
	const freePlay = lab.mode === "free";
	const now = lab.state?.time ?? 0;
	const duration = lab.mode === "trial" ? lab.duration : 0;
	const scripted = lab.controller.source === "script";

	const doneCount = CHALLENGES.filter((c) => progress[c.id]?.done).length;
	const starCount = CHALLENGES.reduce((sum, c) => sum + (progress[c.id]?.stars ?? 0), 0);
	const robotKnowsSelected = !lab.catalog || entry !== undefined;

	const pickerLabel = (challenge: Challenge) => {
		const p = progress[challenge.id];
		const mark = p?.done ? "✓ " : "";
		const stars = p?.stars ? ` ${"★".repeat(p.stars)}` : "";
		const running = challenge.id === lab.running ? "  (loaded)" : "";
		return `${mark}${optionLabel(challenge)}${stars}${running}`;
	};

	let runState = lab.connected ? "Nothing loaded" : "";
	if (loaded) {
		const what = lab.mode === "trial" ? (lab.phase === "done" ? "Trial finished" : "Trial") : "Free play";
		runState = `${what}: ${optionLabel(loaded)}`;
	}

	return (
		<main className="dashboard">
			<header className="status-bar">
				<span className={lab.connected ? "dot dot-on" : "dot"} />
				<span>{lab.connected ? "Robot program running" : "Waiting for robot: click Start in the Driver Station"}</span>
				<span className="progress-summary">
					{doneCount} of {CHALLENGES.length} complete
					{starCount > 0 && ` · ${starCount} ★`}
				</span>
				{robot && lab.connected && (
					<span className="robot-state">
						{robot.eStopped
							? "E-stopped"
							: robot.enabled
								? `Enabled · ${robot.mode === "auto" ? "Autonomous" : robot.mode === "test" ? "Test" : "Teleop"}`
								: "Disabled: commands only run when enabled"}
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
						title="Load this challenge's code and drive the robot yourself"
						onClick={() => lab.run(selected.id, "free")}
					>
						Free play
					</button>
					<button type="button" className="button" disabled={!lab.connected || lab.running === ""} onClick={lab.stop}>
						■ Stop
					</button>
					{freePlay && (
						<>
							<button
								type="button"
								className="button"
								title="A human player rolls a piece into the front of the robot"
								onClick={() => lab.fieldEvent("rollin")}
							>
								Roll in a piece
							</button>
							<button type="button" className="button" onClick={() => lab.fieldEvent("reset")}>
								Reset field
							</button>
						</>
					)}
					<span className="run-state">{runState}</span>
				</div>
				{loaded && !selectedLoaded && (
					<p className="muted">
						The robot is running <strong>{optionLabel(loaded)}</strong>.{" "}
						<button type="button" className="link-button" onClick={() => select(loaded.id)}>
							Show it
						</button>
					</p>
				)}
			</section>

			{lab.error && (
				<div className="banner banner-error" role="alert">
					<strong>Your code stopped with an error.</strong>
					<code className="error-text">{lab.error}</code>
					<span>
						Fix it, click <strong>Restart</strong> in the Driver Station, and it will run again.
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
			{lab.waiting && selectedLoaded && (
				<div className="banner banner-go" role="status">
					<strong>Ready when you are.</strong>
					<span>
						<Inline text={lab.waiting.replace(/(Teleop|Autonomous|Enable)/g, "**$1**")} />
					</span>
				</div>
			)}
			{lab.notice && (
				<div className="banner banner-warn" role="status">
					{lab.notice}
				</div>
			)}

			<section className="card lab">
				<div className="lab-grid">
					<div className="lab-field">
						<h2>
							Field
							{lab.state && <span className="tag tag-plain">score {lab.state.score}</span>}
							{trialRunning && duration > 0 && (
								<span className="trial-clock">
									<span className="trial-clock-bar">
										<span style={{ width: `${Math.min(now / duration, 1) * 100}%` }} />
									</span>
									{now.toFixed(1)} / {duration.toFixed(0)} s
								</span>
							)}
						</h2>
						<FieldView
							state={lab.state}
							pieces={lab.pieces}
							shots={lab.shots}
							path={lab.path}
							start={STARTS[selected.id] ?? SPOT}
						/>
					</div>
					<div className="lab-side">
						<h2>Robot</h2>
						<RobotView state={lab.state} />
						<h2>
							Controller
							{scripted && <span className="tag">the trial is driving</span>}
						</h2>
						<Gamepad controller={lab.controller} onChange={lab.setVirtual} disabled={!lab.connected || scripted} />
					</div>
				</div>
			</section>

			<section className="card">
				<h2>
					Command timeline
					{lab.running && <span className="tag tag-plain">{freePlay ? "last 10 s" : "this trial"}</span>}
				</h2>
				<Timeline timeline={lab.timeline} subsystems={lab.subsystems} now={now} duration={duration} />
				<p className="muted">
					Each lane is a subsystem; each bar is a command that required it. A command that needs two subsystems shows up in
					both. A red end means it was interrupted (hover to see by what).
				</p>
			</section>

			<Results entry={entry} result={result} running={trialActive} />

			<ChallengeDetails key={selected.id} challenge={selected} />

			<details className="card how">
				<summary>How the Command Lab works</summary>
				<ol className="steps">
					<li>
						<Inline text="Each challenge is a class in `challenges/` that extends `CommandChallenge`: a small RobotContainer with the robot's subsystems (`drive`, `intake`, `indexer`, `shooter`, `leds`) and the driver's `controller`. Bind commands in `configureBindings()`." />
					</li>
					<li>
						<Inline text="Save, then click **Restart** in the Driver Station: Java code has to be rebuilt before the robot can use it. The last thing you ran loads again by itself." />
					</li>
					<li>
						<Inline text="**Run trial** loads your code and waits for you to **Enable** the robot. Then a scripted driver presses the buttons, and the trial is graded. Commands only run while the robot is enabled, just like on a real robot." />
					</li>
					<li>
						<Inline text="**Free play** loads your code and lets you drive: with a gamepad, the keyboard (see the Driver Station), or the controller drawn here. Challenges that aren't about driving get simple stick driving for free." />
					</li>
					<li>
						<Inline text="Everything is also logged under `CommandLab/`, `Drive/`, `Shooter/` and friends, so you can study a run in AdvantageScope: try `CommandLab/RobotPose` on a 2D field." />
					</li>
				</ol>
			</details>
		</main>
	);
}
