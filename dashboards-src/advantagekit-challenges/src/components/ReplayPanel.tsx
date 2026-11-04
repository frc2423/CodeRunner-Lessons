import { useEffect, useState } from "react";
import type { Challenge } from "../challenges";
import type { ReplayInfo, TrialResult } from "../lab";
import { type Line, StepPlot } from "./StepPlot";

const REAL_COLOR = "var(--muted)";
const REPLAY_COLOR = "var(--accent)";
const INPUT_COLOR = "var(--teal)";

const inputId = (key: string) => `input:${key}`;

/** Each key's own small plot, with the real run and the replay on top of each other. */
function KeyPlot({ id, replay }: { id: string; replay: ReplayInfo }) {
	const end = Math.max(replay.duration, 1);
	if (id.startsWith("input:")) {
		const key = id.slice("input:".length);
		const lines: Line[] = [{ label: key, color: INPUT_COLOR, points: replay.inputs[key] ?? [] }];
		return <StepPlot title={`${key} (logged input)`} lines={lines} start={0} end={end} height={92} />;
	}
	const series = replay.series[id];
	if (!series) return null;
	const lines: Line[] = [];
	if (series.real) {
		lines.push({ label: "Real run", color: REAL_COLOR, points: series.real, labels: series.realLabels });
	}
	if (series.replay) {
		lines.push({
			label: series.real ? "Replay" : "Replay (new: this was never logged at the match)",
			color: REPLAY_COLOR,
			points: series.replay,
			dashed: Boolean(series.real),
			labels: series.replayLabels,
		});
	}
	return <StepPlot title={id} lines={lines} start={0} end={end} height={92} />;
}

function chooseDefaults(challenge: Challenge, replay: ReplayInfo): string[] {
	const wanted = challenge.panels.replayKeys ?? [];
	const known = wanted.filter((key) => replay.series[key]);
	const first = replay.differences[0]?.key;
	if (first && !known.includes(first) && replay.series[first]) known.unshift(first);
	if (known.length === 0) {
		const any = Object.keys(replay.series)[0];
		if (any) known.push(any);
	}
	return known;
}

/** The verdict on a replay, and a plot of any key to see where the runs split. */
function ReplayResult({ challenge, result, replay }: { challenge: Challenge; result: TrialResult; replay: ReplayInfo }) {
	const [selected, setSelected] = useState<string[]>(() => chooseDefaults(challenge, replay));
	// A new result can have new keys and a new first difference.
	useEffect(() => setSelected(chooseDefaults(challenge, replay)), [result]);

	const toggle = (id: string) =>
		setSelected((current) => (current.includes(id) ? current.filter((item) => item !== id) : [...current, id]));

	const outputKeys = Object.keys(replay.series).sort();
	const inputKeys = Object.keys(replay.inputs).sort();
	const changed = new Set(replay.differences.map((difference) => difference.key));
	const newKeys = new Set(outputKeys.filter((key) => !replay.series[key].real));
	const first = replay.differences[0];
	const matches = replay.differences.length === 0 && !replay.crash;

	return (
		<>
			{replay.crash ? (
				<div className="verdict verdict-bad">
					<strong>Your code crashed in the replay.</strong>
					<code>{replay.crash}</code>
				</div>
			) : matches ? (
				<div className="verdict verdict-good">
					<strong>✓ The replay matched.</strong> Every output came out the same, over {replay.cycles} loops.
					{newKeys.size > 0 && ` Plus ${newKeys.size} new output${newKeys.size === 1 ? "" : "s"}, logged in hindsight.`}
				</div>
			) : (
				<div className="verdict verdict-bad">
					<strong>
						✗ {replay.differences.length} output{replay.differences.length === 1 ? "" : "s"} came out differently.
					</strong>{" "}
					First: <code>{first.key}</code> at {first.time.toFixed(2)} s was <code>{first.real}</code> on the real run, but{" "}
					<code>{first.replay}</code> in the replay.
				</div>
			)}
			{replay.touches > 0 && (
				<div className="verdict verdict-bad">
					<strong>The replay touched the hardware {replay.touches} times</strong>, first from{" "}
					<code>{replay.firstTouch}</code>. There's no hardware in a replay, so it read zero or false.
				</div>
			)}

			{outputKeys.length + inputKeys.length > 0 && (
				<fieldset className="key-picker">
					<legend className="muted">Show on the plots:</legend>
					{outputKeys.map((key) => (
						<label key={key} className={changed.has(key) ? "key-chip key-chip-changed" : "key-chip"}>
							<input type="checkbox" checked={selected.includes(key)} onChange={() => toggle(key)} />
							{key}
							{changed.has(key) && <span className="key-note">differs</span>}
							{newKeys.has(key) && <span className="key-note key-note-new">new</span>}
						</label>
					))}
					{inputKeys.map((key) => (
						<label key={key} className="key-chip key-chip-input">
							<input type="checkbox" checked={selected.includes(inputId(key))} onChange={() => toggle(inputId(key))} />
							{key}
							<span className="key-note key-note-input">input</span>
						</label>
					))}
				</fieldset>
			)}
			{selected.map((id) => (
				<KeyPlot key={id} id={id} replay={replay} />
			))}
		</>
	);
}

/** What the replay of this challenge's latest run showed. */
export function ReplayPanel({
	challenge,
	result,
	replaying,
	logOnly,
}: {
	challenge: Challenge;
	result: TrialResult | undefined;
	replaying: boolean;
	logOnly: boolean;
}) {
	return (
		<section className="card panel">
			<span className="panel-label">Replay{logOnly ? "" : " of your last trial"}</span>
			{replaying ? (
				<p className="replay-waiting">
					<span className="spinner" aria-hidden="true" /> Replaying: a second copy of your program is reading the log, as fast as it can…
				</p>
			) : result?.replay ? (
				<ReplayResult challenge={challenge} result={result} replay={result.replay} />
			) : result?.replayError ? (
				<div className="verdict verdict-bad">
					<strong>The replay didn't finish.</strong>
					<code>{result.replayError}</code>
				</div>
			) : (
				<p className="muted">
					{logOnly
						? "Click Replay the match to run this code on the match's log."
						: "When a trial ends, the lab runs your code a second time, with only the trial's log for company. The two runs get compared here."}
				</p>
			)}
		</section>
	);
}

