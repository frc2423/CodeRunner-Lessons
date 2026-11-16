import { type PointerEvent, useRef, useState } from "react";
import type { Bump, CodeObstacle, Config, DrawnObstacle, Rect, RobotState, Shape, Speeds } from "../playground";

/** 100 SVG units per meter; SVG y grows downward, field y grows upward. */
const S = 100;
/** Meters of arrow per m/s of speed. */
const ARROW_SCALE = 0.35;
/** Drawn obstacles snap to this grid (hold Alt to place freely). */
const SNAP = 0.05;
/** Smaller than this (in meters) and a drag is a click, not a new obstacle. */
const MIN_SIZE = 0.1;

export type Layers = { arrows: boolean; trail: boolean; debug: boolean };

type Props = {
	config: Config;
	state: RobotState | undefined;
	codeObstacles: CodeObstacle[];
	drawn: DrawnObstacle[];
	/** For each drawn obstacle, in order: whether the robot is inside it. */
	drawnInside: boolean[];
	selectedId: string | null;
	onSelect(id: string | null): void;
	onAdd(rect: Rect): void;
	onMove(id: string, cx: number, cy: number): void;
	shapes: Shape[];
	bumps: Bump[];
	trail: [number, number][];
	layers: Layers;
};

type Drag =
	| { kind: "draw"; x0: number; y0: number; x1: number; y1: number }
	| { kind: "move"; id: string; dx: number; dy: number; cx: number; cy: number; moved: boolean };

function corners({ cx, cy, xw, yw, rot }: Rect): [number, number][] {
	const c = Math.cos(rot);
	const s = Math.sin(rot);
	return [
		[xw / 2, yw / 2],
		[-xw / 2, yw / 2],
		[-xw / 2, -yw / 2],
		[xw / 2, -yw / 2],
	].map(([x, y]) => [cx + x * c - y * s, cy + x * s + y * c]);
}

const fmt = (v: number) => (Math.round(v * 100) / 100).toFixed(2);

/**
 * The field from above, like AdvantageScope's 2D field: the robot, the
 * obstacles from Obstacles.java (gray) and from this tab (orange), what the
 * driver asked for and what smart steering did with it, and anything
 * SmartSteering drew with the Debug class.
 *
 * Drag on an empty spot to draw an obstacle; drag a drawn one to move it.
 */
export function FieldView(props: Props) {
	const { config, state, codeObstacles, drawn, drawnInside, selectedId, onSelect, onAdd, onMove, shapes, bumps, trail, layers } =
		props;
	const L = config.fieldLength;
	const W = config.fieldWidth;
	const sx = (x: number) => x * S;
	const sy = (y: number) => (W - y) * S;
	const points = (rect: Rect) =>
		corners(rect)
			.map(([x, y]) => `${sx(x)},${sy(y)}`)
			.join(" ");

	const svgRef = useRef<SVGSVGElement>(null);
	const [drag, setDrag] = useState<Drag | null>(null);
	const [hover, setHover] = useState<{ x: number; y: number } | null>(null);

	const toField = (event: PointerEvent) => {
		const svg = svgRef.current;
		const matrix = svg?.getScreenCTM();
		if (!svg || !matrix) return { x: 0, y: 0 };
		const point = new DOMPoint(event.clientX, event.clientY).matrixTransform(matrix.inverse());
		return { x: point.x / S, y: W - point.y / S };
	};
	const snap = (v: number, event: PointerEvent) => (event.altKey ? v : Math.round(v / SNAP) * SNAP);
	const clampX = (x: number) => Math.max(0, Math.min(L, x));
	const clampY = (y: number) => Math.max(0, Math.min(W, y));

	const down = (event: PointerEvent<SVGSVGElement>) => {
		if (event.button !== 0) return;
		const p = toField(event);
		const target = (event.target as Element).closest("[data-obstacle]");
		const id = target?.getAttribute("data-obstacle");
		const hit = id ? drawn.find((o) => o.id === id) : undefined;
		event.currentTarget.setPointerCapture(event.pointerId);
		if (hit) {
			onSelect(hit.id);
			setDrag({ kind: "move", id: hit.id, dx: hit.cx - p.x, dy: hit.cy - p.y, cx: hit.cx, cy: hit.cy, moved: false });
		} else {
			const x = clampX(snap(p.x, event));
			const y = clampY(snap(p.y, event));
			setDrag({ kind: "draw", x0: x, y0: y, x1: x, y1: y });
		}
	};

	const move = (event: PointerEvent<SVGSVGElement>) => {
		const p = toField(event);
		setHover(p.x >= 0 && p.x <= L && p.y >= 0 && p.y <= W ? p : null);
		if (!drag) return;
		if (drag.kind === "draw") {
			setDrag({ ...drag, x1: clampX(snap(p.x, event)), y1: clampY(snap(p.y, event)) });
		} else {
			const cx = clampX(snap(p.x + drag.dx, event));
			const cy = clampY(snap(p.y + drag.dy, event));
			setDrag({ ...drag, cx, cy, moved: drag.moved || cx !== drag.cx || cy !== drag.cy });
		}
	};

	const up = () => {
		if (!drag) return;
		if (drag.kind === "draw") {
			const xw = Math.abs(drag.x1 - drag.x0);
			const yw = Math.abs(drag.y1 - drag.y0);
			if (xw >= MIN_SIZE && yw >= MIN_SIZE) {
				onAdd({ cx: (drag.x0 + drag.x1) / 2, cy: (drag.y0 + drag.y1) / 2, xw, yw, rot: 0 });
			} else {
				onSelect(null);
			}
		} else if (drag.moved) {
			onMove(drag.id, drag.cx, drag.cy);
		}
		setDrag(null);
	};

	const robot = state ?? {
		x: config.startX,
		y: config.startY,
		heading: config.startHeading,
		touching: false,
	};
	const now = state?.time ?? 0;
	const preview =
		drag?.kind === "draw"
			? {
					cx: (drag.x0 + drag.x1) / 2,
					cy: (drag.y0 + drag.y1) / 2,
					xw: Math.abs(drag.x1 - drag.x0),
					yw: Math.abs(drag.y1 - drag.y0),
					rot: 0,
				}
			: null;

	return (
		<svg
			ref={svgRef}
			className={drag?.kind === "move" ? "field field-moving" : "field"}
			viewBox={`-45 -30 ${L * S + 90} ${W * S + 75}`}
			role="img"
			aria-label="The field from above"
			onPointerDown={down}
			onPointerMove={move}
			onPointerUp={up}
			onPointerCancel={() => setDrag(null)}
			onPointerLeave={() => setHover(null)}
		>
			<rect className="field-carpet" x={0} y={0} width={L * S} height={W * S} />
			{/* A grid line every meter, numbered every two. */}
			{Array.from({ length: Math.floor(L) }, (_, i) => (
				<line key={`gx${i}`} className="field-grid" x1={sx(i + 1)} y1={0} x2={sx(i + 1)} y2={W * S} />
			))}
			{Array.from({ length: Math.floor(W) }, (_, i) => (
				<line key={`gy${i}`} className="field-grid" x1={0} y1={sy(i + 1)} x2={L * S} y2={sy(i + 1)} />
			))}
			{Array.from({ length: Math.floor(L / 2) + 1 }, (_, i) => (
				<text key={`nx${i}`} className="field-tick" x={sx(i * 2)} y={W * S + 26} textAnchor="middle">
					{i * 2}
				</text>
			))}
			{Array.from({ length: Math.floor(W / 2) + 1 }, (_, i) => (
				<text key={`ny${i}`} className="field-tick" x={-12} y={sy(i * 2) + 7} textAnchor="end">
					{i * 2}
				</text>
			))}
			<line className="field-center" x1={sx(L / 2)} y1={0} x2={sx(L / 2)} y2={W * S} />
			<line className="field-wall-blue" x1={0} y1={0} x2={0} y2={W * S} />
			<line className="field-wall-red" x1={L * S} y1={0} x2={L * S} y2={W * S} />
			<rect className="field-border" x={0} y={0} width={L * S} height={W * S} />
			<text className="field-label" transform={`translate(28 ${(W * S) / 2}) rotate(-90)`} textAnchor="middle">
				Blue driver station
			</text>

			{/* Obstacles from Obstacles.java. */}
			{/* They light up red while the robot drives through them. */}
			{codeObstacles.map((rect, i) => (
				<polygon
					key={`c${i}`}
					className={rect.inside ? "obstacle obstacle-code obstacle-hit" : "obstacle obstacle-code"}
					points={points(rect)}
				>
					<title>From Obstacles.java</title>
				</polygon>
			))}

			{/* Obstacles drawn on this tab. */}
			{drawn.map((o, i) => {
				const moving = drag?.kind === "move" && drag.id === o.id;
				const rect = moving ? { ...o, cx: drag.cx, cy: drag.cy } : o;
				const classes = ["obstacle", "obstacle-drawn"];
				if (drawnInside[i] && !moving) classes.push("obstacle-hit");
				if (o.id === selectedId) classes.push("obstacle-selected");
				return (
					<polygon key={o.id} data-obstacle={o.id} className={classes.join(" ")} points={points(rect)}>
						<title>Drawn on this tab: drag to move</title>
					</polygon>
				);
			})}

			{preview && (
				<>
					<polygon className="obstacle obstacle-preview" points={points(preview)} />
					<text className="field-measure" x={sx(preview.cx)} y={sy(preview.cy) + 7} textAnchor="middle">
						{fmt(preview.xw)} × {fmt(preview.yw)} m
					</text>
				</>
			)}

			{layers.trail && trail.length > 1 && (
				<polyline className="trail" points={trail.map(([x, y]) => `${sx(x)},${sy(y)}`).join(" ")} />
			)}

			{/* Where it bumped into something. */}
			{bumps.map((bump) => {
				const age = now - bump.time;
				if (age < 0 || age > 1.2) return null;
				return (
					<circle
						key={`b${bump.time.toFixed(3)}-${bump.x.toFixed(2)}`}
						className="bump"
						cx={sx(bump.x)}
						cy={sy(bump.y)}
						r={10 + age * 70}
						opacity={1 - age / 1.2}
					/>
				);
			})}

			<g
				className={`robot${state ? "" : " robot-idle"}${robot.touching ? " robot-touching" : ""}`}
				transform={`translate(${sx(robot.x)} ${sy(robot.y)}) rotate(${(-robot.heading * 180) / Math.PI})`}
			>
				<rect
					className="robot-body"
					x={(-config.robotLength * S) / 2}
					y={(-config.robotWidth * S) / 2}
					width={config.robotLength * S}
					height={config.robotWidth * S}
					rx={5}
				/>
				<rect
					className="robot-bumper"
					x={(-config.robotLength * S) / 2 + 3}
					y={(-config.robotWidth * S) / 2 + 3}
					width={config.robotLength * S - 6}
					height={config.robotWidth * S - 6}
					rx={4}
				/>
				{/* Which way it faces. */}
				<line className="robot-arrow-line" x1={-config.robotLength * S * 0.22} y1={0} x2={config.robotLength * S * 0.14} y2={0} />
				<path
					className="robot-arrow"
					d={`M ${config.robotLength * S * 0.34} 0 L ${config.robotLength * S * 0.1} -17 L ${config.robotLength * S * 0.1} 17 Z`}
				/>
			</g>

			{state && layers.arrows && (
				<>
					<SpeedArrow state={state} speeds={state.steered} className="arrow-steered" sx={sx} sy={sy} />
					{/* On top and dashed, so both show when steering changed nothing. */}
					{state.smartOn && <SpeedArrow state={state} speeds={state.requested} className="arrow-requested" sx={sx} sy={sy} />}
				</>
			)}

			{layers.debug && shapes.map((shape, i) => <DebugShape key={`d${i}`} shape={shape} sx={sx} sy={sy} points={points} />)}

			{hover && !drag && (
				<text className="field-cursor" x={L * S - 10} y={-8} textAnchor="end">
					x {fmt(hover.x)} m, y {fmt(hover.y)} m
				</text>
			)}
			{drag?.kind === "move" && (
				<text className="field-cursor" x={L * S - 10} y={-8} textAnchor="end">
					center x {fmt(drag.cx)} m, y {fmt(drag.cy)} m
				</text>
			)}
		</svg>
	);
}

type Scale = { sx(x: number): number; sy(y: number): number };

function SpeedArrow({ state, speeds, className, sx, sy }: { state: RobotState; speeds: Speeds; className: string } & Scale) {
	if (Math.hypot(speeds.vx, speeds.vy) < 0.05) return null;
	return (
		<Arrow
			className={className}
			x1={sx(state.x)}
			y1={sy(state.y)}
			x2={sx(state.x + speeds.vx * ARROW_SCALE)}
			y2={sy(state.y + speeds.vy * ARROW_SCALE)}
		/>
	);
}

/** A line with an arrowhead, in SVG units. */
function Arrow({ x1, y1, x2, y2, className }: { x1: number; y1: number; x2: number; y2: number; className: string }) {
	const length = Math.hypot(x2 - x1, y2 - y1);
	if (length < 1) return null;
	const head = Math.min(18, length * 0.6);
	const ux = (x2 - x1) / length;
	const uy = (y2 - y1) / length;
	const bx = x2 - ux * head;
	const by = y2 - uy * head;
	const wing = head * 0.55;
	return (
		<g className={`arrow ${className}`}>
			<line x1={x1} y1={y1} x2={bx} y2={by} />
			<path d={`M ${x2} ${y2} L ${bx - uy * wing} ${by + ux * wing} L ${bx + uy * wing} ${by - ux * wing} Z`} />
		</g>
	);
}

function DebugShape({ shape, sx, sy, points }: { shape: Shape; points(rect: Rect): string } & Scale) {
	const className = `debug debug-${shape.color}`;
	switch (shape.kind) {
		case "point":
			return <circle className={`${className} debug-point`} cx={sx(shape.x)} cy={sy(shape.y)} r={7} />;
		case "line":
			return <line className={className} x1={sx(shape.x1)} y1={sy(shape.y1)} x2={sx(shape.x2)} y2={sy(shape.y2)} />;
		case "arrow":
			return <Arrow className={className} x1={sx(shape.x1)} y1={sy(shape.y1)} x2={sx(shape.x2)} y2={sy(shape.y2)} />;
		case "rectangle":
			return <polygon className={className} points={points(shape.rect)} />;
		case "circle":
			return <circle className={className} cx={sx(shape.x)} cy={sy(shape.y)} r={Math.max(0, shape.r) * S} />;
	}
}
