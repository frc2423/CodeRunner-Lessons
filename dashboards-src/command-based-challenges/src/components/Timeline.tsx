import type { Bar, InputSample, SubsystemInfo, Timeline as TimelineData } from "../lab";

const BASE_LANES = ["Drive", "Intake", "Indexer", "Shooter", "Leds"];
const NO_SUBSYSTEM = "(no subsystem)";

/** Buttons drawn on the controller lane, by their bit in the button mask. */
const BUTTONS: { label: string; test: (s: InputSample) => boolean }[] = [
	{ label: "A", test: (s) => (s.buttons & 1) !== 0 },
	{ label: "B", test: (s) => (s.buttons & 2) !== 0 },
	{ label: "X", test: (s) => (s.buttons & 4) !== 0 },
	{ label: "Y", test: (s) => (s.buttons & 8) !== 0 },
	{ label: "LB", test: (s) => (s.buttons & 16) !== 0 },
	{ label: "RB", test: (s) => (s.buttons & 32) !== 0 },
	{ label: "LT", test: (s) => s.lt > 0.5 },
	{ label: "RT", test: (s) => s.rt > 0.5 },
	{ label: "Back", test: (s) => (s.buttons & 64) !== 0 },
	{ label: "Start", test: (s) => (s.buttons & 128) !== 0 },
	{ label: "▲", test: (s) => s.pov === 0 },
	{ label: "▶", test: (s) => s.pov === 90 },
	{ label: "▼", test: (s) => s.pov === 180 },
	{ label: "◀", test: (s) => s.pov === 270 },
];

/** A steady color per command name, so one command looks the same in every lane. */
function colorFor(name: string): string {
	let h = 0;
	for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) % 360;
	return `hsl(${h} 65% 55%)`;
}

type Segment = { from: number; to: number };

function pressedSegments(inputs: InputSample[], test: (s: InputSample) => boolean, now: number): Segment[] {
	const segments: Segment[] = [];
	let from: number | null = null;
	for (const sample of inputs) {
		if (test(sample)) {
			if (from === null) from = sample.time;
		} else if (from !== null) {
			segments.push({ from, to: sample.time });
			from = null;
		}
	}
	if (from !== null) segments.push({ from, to: now });
	return segments;
}

type Props = {
	timeline: TimelineData;
	subsystems: SubsystemInfo[];
	now: number;
	/** The trial's length, or 0 in free play (then the last 10 s scroll by). */
	duration: number;
};

/**
 * The command scheduler over time: one lane per subsystem, with a bar for
 * every command that used it, and a lane for the controller's buttons. A
 * command that requires two subsystems shows up in both lanes, in the same
 * color. Interrupted commands end in a red notch.
 */
export function Timeline({ timeline, subsystems, now, duration }: Props) {
	const span = duration > 0 ? duration : 10;
	const start = duration > 0 ? 0 : Math.max(0, now - span);
	const end = start + span;
	const pos = (t: number) => `${((Math.max(start, Math.min(end, t)) - start) / span) * 100}%`;
	const width = (a: number, b: number) =>
		`${((Math.max(start, Math.min(end, b)) - Math.max(start, Math.min(end, a))) / span) * 100}%`;

	const lanes: string[] = [...BASE_LANES];
	for (const info of subsystems) if (!lanes.includes(info.name)) lanes.push(info.name);
	for (const bar of timeline.bars) {
		for (const requirement of bar.requirements) if (!lanes.includes(requirement)) lanes.push(requirement);
	}
	const hasLoose = timeline.bars.some((bar) => bar.requirements.length === 0);
	if (hasLoose) lanes.push(NO_SUBSYSTEM);

	const barsFor = (lane: string): Bar[] =>
		timeline.bars.filter((bar) =>
			lane === NO_SUBSYSTEM ? bar.requirements.length === 0 : bar.requirements.includes(lane),
		);

	const buttonRows = BUTTONS.map((button) => ({
		label: button.label,
		segments: pressedSegments(timeline.inputs, button.test, now).filter((s) => s.to >= start && s.from <= end),
	})).filter((row) => row.segments.length > 0);

	const ticks: number[] = [];
	const step = span > 20 ? 10 : span > 8 ? 2 : 1;
	for (let t = Math.ceil(start / step) * step; t <= end + 1e-6; t += step) ticks.push(t);

	return (
		<div className="timeline">
			<div className="timeline-row timeline-axis">
				<div className="timeline-label" />
				<div className="timeline-track">
					{ticks.map((t) => (
						<span key={t} className="timeline-tick" style={{ left: pos(t) }}>
							{t.toFixed(0)} s
						</span>
					))}
				</div>
			</div>

			<div className="timeline-row timeline-controller">
				<div className="timeline-label">
					<strong>Controller</strong>
				</div>
				<div className="timeline-track timeline-buttons">
					{buttonRows.length === 0 && <span className="timeline-empty">no buttons pressed</span>}
					{buttonRows.map((row) => (
						<div key={row.label} className="timeline-button-row">
							{row.segments.map((segment) => (
								<span
									key={`${row.label}${segment.from}`}
									className="timeline-press"
									style={{ left: pos(segment.from), width: width(segment.from, segment.to) }}
									title={`${row.label} held ${segment.from.toFixed(2)}–${segment.to.toFixed(2)} s`}
								>
									{row.label}
								</span>
							))}
						</div>
					))}
				</div>
			</div>

			{lanes.map((lane) => {
				const info = subsystems.find((s) => s.name === lane);
				return (
					<div key={lane} className="timeline-row">
						<div className="timeline-label">
							<strong>{lane}</strong>
							{info?.current ? (
								<span className="timeline-current" style={{ borderColor: colorFor(info.current) }}>
									{info.current}
								</span>
							) : lane !== NO_SUBSYSTEM ? (
								<span className="timeline-idle">idle</span>
							) : null}
							{info?.fallback && <span className="timeline-default">default: {info.fallback}</span>}
						</div>
						<div className="timeline-track">
							{barsFor(lane).map((bar) => {
								const to = bar.end ?? now;
								if (to < start || bar.start > end) return null;
								const instant = to - bar.start < 0.05;
								const title = `${bar.name}: ${bar.start.toFixed(2)}–${bar.end === null ? "now" : `${bar.end.toFixed(2)} s`}${
									bar.requirements.length > 0 ? `\nrequires ${bar.requirements.join(", ")}` : ""
								}${bar.interrupted ? `\ninterrupted${bar.by ? ` by ${bar.by}` : ""}` : ""}`;
								return (
									<span
										key={bar.key}
										className={[
											"timeline-bar",
											instant ? "timeline-instant" : "",
											bar.interrupted ? "timeline-interrupted" : "",
											bar.end === null ? "timeline-running" : "",
										].join(" ")}
										style={{
											left: pos(bar.start),
											width: instant ? undefined : width(bar.start, to),
											background: colorFor(bar.name),
										}}
										title={title}
									>
										{!instant && <span className="timeline-bar-name">{bar.name}</span>}
									</span>
								);
							})}
							<span className="timeline-now" style={{ left: pos(now) }} />
						</div>
					</div>
				);
			})}
		</div>
	);
}
