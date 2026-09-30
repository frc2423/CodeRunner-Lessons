import { type MouseEvent, useEffect, useRef, useState } from "react";
import type { Line } from "../lab";

export type PlotLine = {
	label: string;
	color: string;
	values: Line;
	dashed?: boolean;
	faint?: boolean;
};

export type PlotData = {
	time: number[];
	lines: PlotLine[];
	volts: number[];
};

const MARGIN_LEFT = 52;
const MARGIN_RIGHT = 10;
const TOP_PAD = 18;
const TOP_HEIGHT = 190;
const VOLTS_HEIGHT = 70;
const GAP = 18;
const AXIS_HEIGHT = 24;
const HEIGHT = TOP_PAD + TOP_HEIGHT + GAP + VOLTS_HEIGHT + AXIS_HEIGHT;

function useElementWidth<T extends HTMLElement>() {
	const ref = useRef<T>(null);
	const [width, setWidth] = useState(600);
	useEffect(() => {
		const element = ref.current;
		if (!element) return;
		const observer = new ResizeObserver(([entry]) => {
			setWidth(Math.max(Math.floor(entry.contentRect.width), 240));
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
	const step =
		[1, 2, 2.5, 5, 10].map((m) => m * magnitude).find((s) => s >= raw) ??
		raw;
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

/**
 * A time plot of the goal, the mechanism and the student's lines, with the
 * motor voltage underneath. Hover to read values.
 */
export function Plot({
	data,
	start,
	end,
	unit,
	range,
	decimals,
	overlay,
}: {
	data: PlotData;
	/** Time axis, in seconds. */
	start: number;
	end: number;
	unit: string;
	/** The y range to include at least. */
	range: [number, number];
	decimals: number;
	overlay?: string;
}) {
	const [ref, width] = useElementWidth<HTMLDivElement>();
	const [hover, setHover] = useState<number | null>(null);
	const plotWidth = width - MARGIN_LEFT - MARGIN_RIGHT;

	let low = range[0];
	let high = range[1];
	for (const line of data.lines) {
		for (const value of line.values) {
			if (value === null || !Number.isFinite(value)) continue;
			low = Math.min(low, value);
			high = Math.max(high, value);
		}
	}
	const pad = (high - low) * 0.06 || 1;
	low -= pad;
	high += pad;

	const x = (t: number) =>
		MARGIN_LEFT + ((t - start) / Math.max(end - start, 1e-6)) * plotWidth;
	const y = (v: number) => TOP_PAD + TOP_HEIGHT - ((v - low) / (high - low)) * TOP_HEIGHT;
	const voltsTop = TOP_PAD + TOP_HEIGHT + GAP;
	const yVolts = (v: number) => voltsTop + ((12 - v) / 24) * VOLTS_HEIGHT;

	const path = (values: Line, toY: (v: number) => number) => {
		let d = "";
		let drawing = false;
		for (let i = 0; i < data.time.length; i++) {
			const value = values[i];
			const t = data.time[i];
			if (value === null || value === undefined || !Number.isFinite(value) || t < start) {
				drawing = false;
				continue;
			}
			const clamped = Math.min(Math.max(toY(value), -20), HEIGHT + 20);
			d += `${drawing ? "L" : "M"}${x(t).toFixed(1)},${clamped.toFixed(1)}`;
			drawing = true;
		}
		return d;
	};

	const onMove = (event: MouseEvent<SVGSVGElement>) => {
		const box = event.currentTarget.getBoundingClientRect();
		const t = start + ((event.clientX - box.left - MARGIN_LEFT) / plotWidth) * (end - start);
		if (data.time.length === 0 || t < start || t > end) {
			setHover(null);
			return;
		}
		let best = 0;
		for (let i = 1; i < data.time.length; i++) {
			if (Math.abs(data.time[i] - t) < Math.abs(data.time[best] - t)) best = i;
		}
		setHover(best);
	};

	const xTicks = ticks(start, end, Math.max(Math.floor(plotWidth / 70), 2));
	const yTicks = ticks(low, high, 5);
	const hoverTime = hover !== null ? data.time[hover] : undefined;

	return (
		<div className="plot" ref={ref}>
			<svg
				width={width}
				height={HEIGHT}
				role="img"
				aria-label="Plot of the goal, the mechanism and the voltage over time"
				onMouseMove={onMove}
				onMouseLeave={() => setHover(null)}
			>
				{/* Main panel */}
				{yTicks.map((tick) => (
					<g key={`y${tick}`}>
						<line x1={MARGIN_LEFT} x2={width - MARGIN_RIGHT} y1={y(tick)} y2={y(tick)} className="grid" />
						<text x={MARGIN_LEFT - 6} y={y(tick) + 4} className="axis-label" textAnchor="end">
							{formatTick(tick)}
						</text>
					</g>
				))}
				<text x={MARGIN_LEFT - 6} y={10} className="axis-unit" textAnchor="end">
					{unit}
				</text>
				{data.lines.map((line) => (
					<path
						key={line.label}
						d={path(line.values, y)}
						fill="none"
						stroke={line.color}
						strokeWidth={line.faint ? 1 : 2}
						strokeOpacity={line.faint ? 0.45 : 1}
						strokeDasharray={line.dashed ? "6 4" : undefined}
						strokeLinejoin="round"
					/>
				))}

				{/* Voltage panel */}
				{[12, 0, -12].map((tick) => (
					<g key={`v${tick}`}>
						<line
							x1={MARGIN_LEFT}
							x2={width - MARGIN_RIGHT}
							y1={yVolts(tick)}
							y2={yVolts(tick)}
							className={tick === 0 ? "grid grid-zero" : "grid grid-limit"}
						/>
						<text x={MARGIN_LEFT - 6} y={yVolts(tick) + 4} className="axis-label" textAnchor="end">
							{tick}
						</text>
					</g>
				))}
				<text x={4} y={voltsTop + VOLTS_HEIGHT / 2 + 4} className="axis-unit">
					V
				</text>
				<path d={path(data.volts, yVolts)} fill="none" className="volts-line" strokeWidth={1.5} />

				{/* Time axis */}
				{xTicks.map((tick) => (
					<text
						key={`x${tick}`}
						x={x(tick)}
						y={HEIGHT - 4}
						className="axis-label"
						textAnchor="middle"
					>
						{formatTick(tick)} s
					</text>
				))}

				{hoverTime !== undefined && (
					<line x1={x(hoverTime)} x2={x(hoverTime)} y1={0} y2={voltsTop + VOLTS_HEIGHT} className="cursor" />
				)}
			</svg>

			{overlay && <div className="plot-overlay">{overlay}</div>}

			{hover !== null && hoverTime !== undefined && (
				<div
					className="plot-readout"
					style={x(hoverTime) > width / 2 ? { left: 60 } : { right: 14 }}
				>
					<strong>{hoverTime.toFixed(2)} s</strong>
					{data.lines.map((line) => {
						const value = line.values[hover];
						if (value === null || value === undefined || !Number.isFinite(value)) return null;
						return (
							<span key={line.label}>
								<i style={{ background: line.color }} /> {line.label}:{" "}
								{value.toFixed(decimals)}
							</span>
						);
					})}
					<span>
						<i className="volts-swatch" /> Volts: {data.volts[hover]?.toFixed(2)}
					</span>
				</div>
			)}

			<div className="legend">
				{data.lines.map((line) => (
					<span key={line.label} className={line.faint ? "legend-faint" : undefined}>
						<i
							style={{
								background: line.dashed ? "transparent" : line.color,
								borderColor: line.color,
							}}
							className={line.dashed ? "swatch-dashed" : undefined}
						/>
						{line.label}
					</span>
				))}
				<span>
					<i className="volts-swatch" />
					Motor voltage
				</span>
			</div>
		</div>
	);
}
