import type { Points } from "../lab";

const PREFIX = "output:CommandsAll/";

function bars(points: Points, end: number): [number, number][] {
	const out: [number, number][] = [];
	let from: number | null = null;
	for (let i = 0; i + 1 < points.length; i += 2) {
		const on = points[i + 1] === 1;
		if (on && from === null) from = points[i];
		if (!on && from !== null) {
			out.push([from, points[i]]);
			from = null;
		}
	}
	if (from !== null) out.push([from, end]);
	return out;
}

/**
 * The `CommandsAll/` keys the student logged, one lane each. A bar that never
 * ends is a command whose stop never reached the log.
 */
export function CommandTimeline({ trace, end }: { trace: Record<string, Points>; end: number }) {
	const lanes = Object.keys(trace)
		.filter((id) => id.startsWith(PREFIX))
		.sort();
	const span = Math.max(end, 1);
	const seconds = Array.from({ length: Math.floor(span) + 1 }, (_, i) => i);

	return (
		<section className="card panel">
			<span className="panel-label">Command timeline: drawn from your log</span>
			{lanes.length === 0 ? (
				<p className="muted">
					No commands here yet. Log <code>CommandsAll/&lt;name&gt;</code> as true and false, and they'll appear as bars.
				</p>
			) : (
				<div className="timeline">
					{lanes.map((id) => {
						const name = id.slice(PREFIX.length);
						return (
							<div key={id} className="lane">
								<span className="lane-name">{name}</span>
								<div className="lane-track">
									{bars(trace[id], end).map(([from, to]) => (
										<span
											key={from}
											className="lane-bar"
											title={`${name}: ${from.toFixed(2)} s to ${to.toFixed(2)} s`}
											style={{
												left: `${(from / span) * 100}%`,
												width: `${(Math.max(to - from, 0.03) / span) * 100}%`,
											}}
										/>
									))}
								</div>
							</div>
						);
					})}
					<div className="lane lane-axis">
						<span className="lane-name" />
						<div className="lane-track lane-track-axis">
							{seconds.map((second) => (
								<span key={second} style={{ left: `${(second / span) * 100}%` }}>
									{second}s
								</span>
							))}
						</div>
					</div>
				</div>
			)}
		</section>
	);
}
