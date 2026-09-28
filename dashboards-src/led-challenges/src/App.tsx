import { useEffect, useState } from "react";
import {
	CHALLENGES,
	type Challenge,
	findChallenge,
	type Level,
} from "./challenges";
import { useCodeRunnerContext, useRobotState } from "./coderunner/hooks";
import { ChallengeDetails } from "./components/ChallengeDetails";
import { Checklist } from "./components/Checklist";
import { GoalPreview } from "./components/GoalPreview";
import { Inline } from "./components/Inline";
import { LedStripView } from "./components/LedStripView";
import { DEFAULT_LED_COUNT } from "./leds";
import { useRobotLeds } from "./robot";

const STORAGE_KEY = "led-challenges:selected";

/** Browser storage can be missing or blocked in a sandboxed dashboard. */
function loadSelected(): string {
	try {
		const id = window.localStorage.getItem(STORAGE_KEY);
		if (findChallenge(id ?? undefined)) return id as string;
	} catch {}
	return CHALLENGES[0].id;
}

function saveSelected(id: string): void {
	try {
		window.localStorage.setItem(STORAGE_KEY, id);
	} catch {}
}

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

function optionLabel(challenge: Challenge): string {
	return challenge.number > 0
		? `${challenge.number}. ${challenge.title}`
		: challenge.title;
}

export default function App() {
	const context = useCodeRunnerContext();
	const robot = useRobotState();
	const leds = useRobotLeds();

	const [selectedId, setSelectedId] = useState(loadSelected);
	const selected = findChallenge(selectedId) ?? CHALLENGES[0];
	const index = CHALLENGES.indexOf(selected);
	const running = findChallenge(leds.running);
	const selectedIsRunning = leds.running === selected.id;
	const count = leds.colors?.length || DEFAULT_LED_COUNT;
	const robotKnowsSelected =
		leds.knownIds === undefined || leds.knownIds.includes(selected.id);

	// Bumped on every Run, so the goal animation restarts in step with the robot.
	const [runKey, setRunKey] = useState(0);

	const select = (id: string) => {
		setSelectedId(id);
		saveSelected(id);
	};

	const runSelected = () => {
		leds.run(selected.id);
		setRunKey((key) => key + 1);
	};

	// Follow CodeRunner's light/dark theme.
	useEffect(() => {
		document.documentElement.dataset.theme = context?.theme ?? "dark";
	}, [context?.theme]);

	let robotOverlay: string | undefined;
	if (!leds.connected) {
		robotOverlay = "Waiting for the robot: click Start in the Driver Station";
	} else if (!leds.colors) {
		robotOverlay = "Waiting for LED data…";
	}

	return (
		<main className="dashboard">
			<header className="status-bar">
				<span className={leds.connected ? "dot dot-on" : "dot"} />
				<span>
					{leds.connected
						? "Robot program running"
						: "Waiting for robot: click Start in the Driver Station"}
				</span>
				{robot && leds.connected && (
					<span className="robot-state">
						{robot.eStopped
							? "E-stopped"
							: robot.enabled
								? `Enabled · ${robot.mode}`
								: "Disabled (LEDs work anyway)"}
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
					<select
						aria-label="Challenge"
						value={selected.id}
						onChange={(event) => select(event.target.value)}
					>
						{GROUPS.map(([level, challenges]) => (
							<optgroup key={level} label={level}>
								{challenges.map((challenge) => (
									<option key={challenge.id} value={challenge.id}>
										{optionLabel(challenge)}
										{challenge.id === leds.running ? "  (running)" : ""}
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
						disabled={!leds.connected}
						title={
							leds.connected
								? "Run this challenge on the robot"
								: "Start the robot program in the Driver Station first"
						}
						onClick={runSelected}
					>
						▶ Run
					</button>
					<button
						type="button"
						className="button"
						disabled={!leds.connected || leds.running === ""}
						onClick={leds.stop}
					>
						■ Stop
					</button>
					<span className="run-state">
						{running
							? `Running: ${optionLabel(running)}`
							: leds.connected
								? "Nothing running"
								: ""}
					</span>
				</div>
			</section>

			{leds.error && (
				<div className="banner banner-error" role="alert">
					<strong>Your challenge stopped with an error.</strong>
					<code className="error-text">{leds.error}</code>
					<span>
						Fix it in the file, click <strong>Restart</strong> in the Driver
						Station, and it will run again.
					</span>
				</div>
			)}
			{leds.stalled && (
				<div className="banner banner-warn" role="alert">
					<strong>The robot stopped responding.</strong>
					<span>
						Is there a loop that never ends (like a <code>while</code> loop
						whose condition never becomes false)? Fix it and click{" "}
						<strong>Restart</strong> in the Driver Station.
					</span>
				</div>
			)}
			{leds.connected && !robotKnowsSelected && (
				<div className="banner banner-warn">
					The robot program doesn't have this challenge. Is{" "}
					<code>LedChallenges.java</code> missing a line for it?
				</div>
			)}

			<section className="card strips">
				<div className="strip-block">
					<h2>
						Your robot
						{running && !selectedIsRunning && (
							<span className="tag">showing {optionLabel(running)}</span>
						)}
					</h2>
					<LedStripView
						colors={leds.colors}
						count={count}
						overlay={robotOverlay}
					/>
				</div>
				<div className="strip-block">
					<h2>
						Goal
						{selected.goalNote && (
							<span className="tag tag-plain">{selected.goalNote}</span>
						)}
					</h2>
					<GoalPreview
						challenge={selected}
						count={count}
						robot={robot}
						runKey={runKey}
					/>
				</div>
				<Checklist
					challenge={selected}
					colors={leds.colors}
					active={selectedIsRunning}
				/>
			</section>

			<ChallengeDetails key={selected.id} challenge={selected} />

			<details className="card how">
				<summary>How the LED challenges work</summary>
				<ol className="steps">
					<li>
						<Inline text="Pick a challenge above and open its file. Every challenge is a class in `subsystems/LEDS/` that `implements Led`." />
					</li>
					<li>
						<Inline text="Write your code in its `update` method. The robot calls `update` about 50 times a second while the challenge runs." />
					</li>
					<li>
						<Inline text="Save, then click **Restart** in the Driver Station. Java code has to be rebuilt before the robot can use it." />
					</li>
					<li>
						<Inline text="Click **Run**. Compare **Your robot** with the **Goal**. After a restart, the last challenge you ran starts again by itself." />
					</li>
					<li>
						<Inline text="The LEDs work while the robot is disabled, so you don't need to enable it (except to test Robot Status)." />
					</li>
				</ol>
			</details>
		</main>
	);
}
