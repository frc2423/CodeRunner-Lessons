import { type MouseEvent, useEffect, useRef, useState } from "react";
import type { Points } from "../lab";

export type Line = {
	label: string;
	color: string;
	points: Points;
	dashed?: boolean;
	/** Names for a line of text values: the value is an index into this. */
	labels?: string[];
};

const MARGIN_LEFT = 52;
const MARGIN_RIGHT = 12;
const TOP = 8;
const AXIS = 20;

function useElementWidth<T extends HTMLElement>() {
	const ref = useRef<T>(null);
	const [width, setWidth] = useState(600);
	useEffect(() => {
		const element = ref.current;
		if (!element) return;
		const observer = new ResizeObserver(([entry]) => {
			setWidth(Math.max(Math.floor(entry.contentRect.width), 220));
		});
		observer.observe(element);
		return () => observer.disconnect();
	}, []);
	return [ref, width] as const;
}

/** Round numbers for axis ticks between `min` and `max`. */
function ticks(min: number, max: number, count: number): number[] {
	const span = max - min;
	if (!(span > 0)) return [min];
	const raw = span / count;
	const magnitude = 10 ** Math.floor(Math.log10(raw));
	const step = [1, 2, 2.5, 5, 10].map((m) => m * magnitude).find((s) => s >= raw) ?? raw;
	const result: number[] = [];
	for (let v = Math.ceil(min / step) * step; v <= max + step * 1e-6; v += step) {
		result.push(Math.abs(v) < step * 1e-6 ? 0 : v);
	}
	return result;
}

function formatTick(value: number): string {
	const abs = Math.abs(value);
	if (abs < 1e-9) return "0";
	if (abs >= 1000) return `${Math.round(value)}`;
	if (abs >= 10) return value.toFixed(0);
	if (abs >= 1) return value.toFixed(1).replace(/\.0$/, "");
	return value.toFixed(2).replace(/0$/, "");
}

/** A line's value at `time`: the last value set at or before it. */
function valueAt(points: Points, time: number): number | undefined {
	let found: number | undefined;
	for (let i = 0; i + 1 < points.length; i += 2) {
		if (points[i] > time) break;
		found = points[i + 1];
	}
	return found;
}

/**
 * A plot of logged values. A value in a log holds until the next one, so lines
 * go in steps. Hover to read the values.
 */
export function StepPlot({
	title,
	unit,
	lines,
	start,
	end,
	range,
	height = 130,
	empty,
}: {
	title?: string;
	unit?: string;
	lines: Line[];
	start: number;
	end: number;
	range?: [number, number];
	height?: number;
	/** Shown across the plot when no line has anything to draw. */
	empty?: string;
}) {
	const [ref, width] = useElementWidth<HTMLDivElement>();
	const [hover, setHover] = useState<number | null>(null);
	const plotWidth = width - MARGIN_LEFT - MARGIN_RIGHT;
	const plotHeight = height - TOP - AXIS;

	let low = range?.[0] ?? Number.POSITIVE_INFINITY;
	let high = range?.[1] ?? Number.NEGATIVE_INFINITY;
	for (const line of lines) {
		for (let i = 1; i < line.points.length; i += 2) {
			const value = line.points[i];
			if (!Number.isFinite(value)) continue;
			low = Math.min(low, value);
			high = Math.max(high, value);
		}
	}
	if (!Number.isFinite(low) || !Number.isFinite(high)) {
		low = 0;
		high = 1;
	}
	if (low === high) {
		low -= 1;
		high += 1;
	}
	const pad = (high - low) * 0.08;
	low -= pad;
	high += pad;

	const x = (t: number) => MARGIN_LEFT + ((t - start) / Math.max(end - start, 1e-6)) * plotWidth;
	const y = (v: number) => TOP + plotHeight - ((v - low) / (high - low)) * plotHeight;

	const path = (points: Points) => {
		let d = "";
		let previous: number | null = null;
		for (let i = 0; i + 1 < points.length; i += 2) {
			const t = Math.max(points[i], start);
			const value = points[i + 1];
			if (!Number.isFinite(value)) continue;
			if (previous === null) d += `M${x(t).toFixed(1)},${y(value).toFixed(1)}`;
			else d += `H${x(t).toFixed(1)}V${y(value).toFixed(1)}`;
			previous = value;
		}
		if (previous !== null) {
			const last = Math.max(points[points.length - 2], start);
			d += `H${x(Math.max(last, end)).toFixed(1)}`;
		}
		return d;
	};

	const onMove = (event: MouseEvent<SVGSVGElement>) => {
		const box = event.currentTarget.getBoundingClientRect();
		const t = start + ((event.clientX - box.left - MARGIN_LEFT) / plotWidth) * (end - start);
		setHover(t >= start && t <= end ? t : null);
	};

	const hasData = lines.some((line) => line.points.length > 0);

	return (
		<div className="plot" ref={ref}>
			{title && (
				<div className="plot-title">
					{title}
					{unit && <span className="muted"> ({unit})</span>}
				</div>
			)}
			<svg
				width={width}
				height={height}
				role="img"
				aria-label={title ?? "A plot of logged values"}
				onMouseMove={onMove}
				onMouseLeave={() => setHover(null)}
			>
				{ticks(low, high, Math.max(Math.floor(plotHeight / 34), 2)).map((tick) => (
					<g key={`y${tick}`}>
						<line x1={MARGIN_LEFT} x2={width - MARGIN_RIGHT} y1={y(tick)} y2={y(tick)} className="grid" />
						<text x={MARGIN_LEFT - 6} y={y(tick) + 4} className="axis-label" textAnchor="end">
							{formatTick(tick)}
						</text>
					</g>
				))}
				{lines.map((line) => (
					<path
						key={line.label}
						d={path(line.points)}
						fill="none"
						stroke={line.color}
						strokeWidth={line.dashed ? 1.5 : 2}
						strokeDasharray={line.dashed ? "6 4" : undefined}
						strokeLinejoin="round"
					/>
				))}
				{ticks(start, end, Math.max(Math.floor(plotWidth / 70), 2)).map((tick) => (
					<text key={`x${tick}`} x={x(tick)} y={height - 4} className="axis-label" textAnchor="middle">
						{formatTick(tick)} s
					</text>
				))}
				{hover !== null && <line x1={x(hover)} x2={x(hover)} y1={TOP} y2={TOP + plotHeight} className="cursor" />}
			</svg>

			{!hasData && empty && <div className="plot-overlay">{empty}</div>}

			{hover !== null && hasData && (
				<div className="plot-readout" style={x(hover) > width / 2 ? { left: 60 } : { right: 14 }}>
					<strong>{hover.toFixed(2)} s</strong>
					{lines.map((line) => {
						const value = valueAt(line.points, hover);
						if (value === undefined) return null;
						return (
							<span key={line.label}>
								<i style={{ background: line.color }} /> {line.label}:{" "}
								{line.labels ? (line.labels[value] ?? value) : Number(value.toPrecision(5))}
							</span>
						);
					})}
				</div>
			)}

			<div className="legend">
				{lines.map((line) => (
					<span key={line.label}>
						<i
							style={{ background: line.dashed ? "transparent" : line.color, borderColor: line.color }}
							className={line.dashed ? "swatch-dashed" : undefined}
						/>
						{line.label}
					</span>
				))}
			</div>
		</div>
	);
}
