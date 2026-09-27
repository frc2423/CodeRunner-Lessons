import { useEffect, useState } from "react";

type Series = { label: string; topic: string; color: string };

const SAMPLE_MS = 50;
const WIDTH = 400;
const HEIGHT = 140;

/**
 * A rolling line chart of numeric topics. Samples the latest values on a
 * fixed interval so every series shares one time axis.
 */
export function HistoryChart({
	series,
	seconds = 10,
	max,
}: {
	series: Series[];
	seconds?: number;
	/** Top of the y axis; the bottom is 0. */
	max: number;
}) {
	const capacity = Math.round((seconds * 1000) / SAMPLE_MS);
	const [history, setHistory] = useState<number[][]>(() => series.map(() => []));

	useEffect(() => {
		const timer = window.setInterval(() => {
			setHistory((previous) =>
				series.map((s, i) => {
					const value = window.coderunner?.nt.getValue<number>(s.topic);
					const next = [...(previous[i] ?? []), typeof value === "number" ? value : 0];
					return next.length > capacity ? next.slice(next.length - capacity) : next;
				}),
			);
		}, SAMPLE_MS);
		return () => window.clearInterval(timer);
	}, [series, capacity]);

	const toPoints = (values: number[]) =>
		values
			.map((value, i) => {
				const x = (i / (capacity - 1)) * WIDTH;
				const y = HEIGHT - (Math.min(Math.max(value, 0), max) / max) * HEIGHT;
				return `${x.toFixed(1)},${y.toFixed(1)}`;
			})
			.join(" ");

	return (
		<div className="chart">
			<svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} preserveAspectRatio="none" role="img">
				<title>Last {seconds} seconds</title>
				{[0.25, 0.5, 0.75].map((fraction) => (
					<line
						key={fraction}
						x1={0}
						x2={WIDTH}
						y1={HEIGHT * fraction}
						y2={HEIGHT * fraction}
						className="grid"
					/>
				))}
				{series.map((s, i) => (
					<polyline
						key={s.topic}
						points={toPoints(history[i] ?? [])}
						fill="none"
						stroke={s.color}
						strokeWidth={2}
						vectorEffect="non-scaling-stroke"
					/>
				))}
			</svg>
			<div className="legend">
				{series.map((s) => (
					<span key={s.topic}>
						<i style={{ background: s.color }} /> {s.label}
					</span>
				))}
				<span className="muted">last {seconds} s · 0–{max} RPM</span>
			</div>
		</div>
	);
}
