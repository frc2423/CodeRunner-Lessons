import { type MouseEvent, useEffect, useRef, useState } from "react";
import type { MechanismKind } from "../lab";

const WIDTH = 320;
const HEIGHT = 240;

type Props = {
	kind: MechanismKind;
	/** Robot units (see Mechanism.java). Null before any data arrives. */
	position: number | null;
	velocity: number | null;
	goal: number | null;
	payload: boolean;
	shots: number;
	/** Called with a goal in robot units when the student clicks (sandbox only). */
	onPickGoal?: (goal: number) => void;
};

/** Where a click landed, in SVG coordinates. */
function svgPoint(event: MouseEvent<SVGSVGElement>) {
	const box = event.currentTarget.getBoundingClientRect();
	return {
		x: ((event.clientX - box.left) / box.width) * WIDTH,
		y: ((event.clientY - box.top) / box.height) * HEIGHT,
	};
}

export function MechanismView(props: Props) {
	const { kind, onPickGoal } = props;
	const interactive = onPickGoal !== undefined;

	const onClick = (event: MouseEvent<SVGSVGElement>) => {
		if (!onPickGoal) return;
		const goal = pickGoal(kind, svgPoint(event));
		if (goal !== null) onPickGoal(goal);
	};

	return (
		<svg
			className={interactive ? "mechanism mechanism-interactive" : "mechanism"}
			viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
			role="img"
			aria-label={`Simulated ${kind}`}
			onClick={onClick}
		>
			{kind === "flywheel" && <Flywheel {...props} />}
			{kind === "steering" && <Steering {...props} />}
			{kind === "elevator" && <Elevator {...props} />}
			{kind === "arm" && <Arm {...props} />}
		</svg>
	);
}

function pickGoal(kind: MechanismKind, p: { x: number; y: number }): number | null {
	switch (kind) {
		case "flywheel":
			if (p.x < GAUGE.x - 30) return null;
			return Math.round(clamp(gaugeValue(p.y), 0, 5500) / 50) * 50;
		case "steering": {
			const degrees = (Math.atan2(STEER.cy - p.y, p.x - STEER.cx) * 180) / Math.PI - 90;
			return Math.round(wrap(degrees));
		}
		case "elevator":
			return Math.round(clamp(elevatorHeight(p.y), 0, 1.6) * 100) / 100;
		case "arm": {
			const radians = Math.atan2(ARM.cy - p.y, p.x - ARM.cx);
			const degrees = Math.round((radians * 180) / Math.PI);
			// Clicks below the pivot on the back side mean "all the way down".
			const fixed = degrees < -135 ? degrees + 360 : degrees;
			return (clamp(fixed, -45, 135) * Math.PI) / 180;
		}
	}
}

const clamp = (v: number, lo: number, hi: number) => Math.min(Math.max(v, lo), hi);
const wrap = (degrees: number) => ((((degrees + 180) % 360) + 360) % 360) - 180;

// Flywheel ------------------------------------------------------------------------

const WHEEL = { cx: 110, cy: 120, r: 72 };
const GAUGE = { x: 250, top: 30, bottom: 210, max: 6000 };
const gaugeY = (rpm: number) =>
	GAUGE.bottom - (clamp(rpm, 0, GAUGE.max) / GAUGE.max) * (GAUGE.bottom - GAUGE.top);
const gaugeValue = (y: number) =>
	((GAUGE.bottom - y) / (GAUGE.bottom - GAUGE.top)) * GAUGE.max;

/** Spins the wheel drawing at a slowed-down rate, so fast speeds stay readable. */
function useSpin(rpm: number | null): number {
	const [angle, setAngle] = useState(0);
	const speed = useRef(0);
	speed.current = rpm ?? 0;
	useEffect(() => {
		let frame = 0;
		let last = performance.now();
		const tick = (now: number) => {
			const dt = Math.min((now - last) / 1000, 0.1);
			last = now;
			// 3000 RPM draws as one turn per second.
			setAngle((a) => (a + (speed.current / 3000) * 360 * dt) % 360);
			frame = requestAnimationFrame(tick);
		};
		frame = requestAnimationFrame(tick);
		return () => cancelAnimationFrame(frame);
	}, []);
	return angle;
}

function Flywheel({ velocity, goal, shots }: Props) {
	const angle = useSpin(velocity);
	const rpm = velocity ?? 0;
	const spokes = [0, 60, 120, 180, 240, 300];
	return (
		<>
			<circle cx={WHEEL.cx} cy={WHEEL.cy} r={WHEEL.r + 6} className="part-frame" />
			<g transform={`rotate(${angle} ${WHEEL.cx} ${WHEEL.cy})`}>
				<circle cx={WHEEL.cx} cy={WHEEL.cy} r={WHEEL.r} className="part-wheel" />
				{spokes.map((a) => (
					<line
						key={a}
						x1={WHEEL.cx}
						y1={WHEEL.cy}
						x2={WHEEL.cx + WHEEL.r * 0.85 * Math.cos((a * Math.PI) / 180)}
						y2={WHEEL.cy + WHEEL.r * 0.85 * Math.sin((a * Math.PI) / 180)}
						className="part-spoke"
					/>
				))}
				<circle cx={WHEEL.cx + WHEEL.r * 0.7} cy={WHEEL.cy} r={6} className="part-marker" />
			</g>
			<circle cx={WHEEL.cx} cy={WHEEL.cy} r={8} className="part-hub" />
			{/* A ball flies out on every shot. */}
			{shots > 0 && (
				<circle key={shots} cx={WHEEL.cx} cy={WHEEL.cy - WHEEL.r - 12} r={11} className="ball ball-fly" />
			)}
			<text x={WHEEL.cx} y={HEIGHT - 6} textAnchor="middle" className="mech-readout">
				{Math.round(rpm)} RPM
			</text>

			{/* Speed gauge */}
			<rect x={GAUGE.x - 12} y={GAUGE.top} width={24} height={GAUGE.bottom - GAUGE.top} rx={6} className="gauge-track" />
			<rect
				x={GAUGE.x - 12}
				y={gaugeY(rpm)}
				width={24}
				height={Math.max(GAUGE.bottom - gaugeY(rpm), 0)}
				rx={6}
				className="gauge-fill"
			/>
			{[0, 2000, 4000, 6000].map((tick) => (
				<text key={tick} x={GAUGE.x + 18} y={gaugeY(tick) + 4} className="mech-label">
					{tick / 1000}k
				</text>
			))}
			{goal !== null && (
				<g>
					<line x1={GAUGE.x - 20} x2={GAUGE.x + 16} y1={gaugeY(goal)} y2={gaugeY(goal)} className="goal-line" />
					<text x={GAUGE.x - 24} y={gaugeY(goal) + 4} textAnchor="end" className="mech-label goal-text">
						goal
					</text>
				</g>
			)}
		</>
	);
}

// Swerve steering -------------------------------------------------------------------

const STEER = { cx: 160, cy: 118, r: 92 };

/** A swerve wheel seen from above; 0° points up (forward), positive turns left. */
function SteerWheel({ degrees, className }: { degrees: number; className: string }) {
	return (
		<g transform={`rotate(${-degrees} ${STEER.cx} ${STEER.cy})`} className={className}>
			<rect x={STEER.cx - 16} y={STEER.cy - 52} width={32} height={104} rx={10} />
			<polygon points={`${STEER.cx},${STEER.cy - 78} ${STEER.cx - 12},${STEER.cy - 60} ${STEER.cx + 12},${STEER.cy - 60}`} />
		</g>
	);
}

function Steering({ position, goal }: Props) {
	const marks = [0, 90, 180, -90];
	return (
		<>
			<circle cx={STEER.cx} cy={STEER.cy} r={STEER.r} className="part-frame" />
			{marks.map((m) => {
				const a = ((m + 90) * Math.PI) / 180;
				return (
					<text
						key={m}
						x={STEER.cx + (STEER.r + 14) * Math.cos(a)}
						y={STEER.cy - (STEER.r + 14) * Math.sin(a) + 4}
						textAnchor="middle"
						className="mech-label"
					>
						{m === 180 ? "±180°" : `${m}°`}
					</text>
				);
			})}
			{goal !== null && <SteerWheel degrees={goal} className="ghost" />}
			{position !== null && <SteerWheel degrees={position} className="part-steer" />}
			<circle cx={STEER.cx} cy={STEER.cy} r={5} className="part-hub" />
			<text x={8} y={HEIGHT - 6} className="mech-readout">
				{position !== null ? `${position.toFixed(1)}°` : ""}
			</text>
		</>
	);
}

// Elevator ---------------------------------------------------------------------------

const LIFT = { left: 110, right: 190, top: 22, bottom: 222, max: 1.6, carriage: 26 };
const elevatorY = (h: number) =>
	LIFT.bottom - LIFT.carriage - (clamp(h, 0, LIFT.max) / LIFT.max) * (LIFT.bottom - LIFT.top - LIFT.carriage);
const elevatorHeight = (y: number) =>
	((LIFT.bottom - LIFT.carriage - y) / (LIFT.bottom - LIFT.top - LIFT.carriage)) * LIFT.max;

function Elevator({ position, goal, payload }: Props) {
	const h = position ?? 0;
	const y = elevatorY(h);
	return (
		<>
			<rect x={LIFT.left - 8} y={LIFT.top} width={8} height={LIFT.bottom - LIFT.top} className="part-rail" />
			<rect x={LIFT.right} y={LIFT.top} width={8} height={LIFT.bottom - LIFT.top} className="part-rail" />
			<line x1={LIFT.left - 30} x2={LIFT.right + 30} y1={LIFT.bottom} y2={LIFT.bottom} className="part-ground" />
			{[0, 0.4, 0.8, 1.2, 1.6].map((tick) => (
				<text key={tick} x={LIFT.left - 16} y={elevatorY(tick) + LIFT.carriage / 2 + 4} textAnchor="end" className="mech-label">
					{tick.toFixed(1)} m
				</text>
			))}
			{goal !== null && (
				<rect
					x={LIFT.left + 2}
					y={elevatorY(goal)}
					width={LIFT.right - LIFT.left - 4}
					height={LIFT.carriage}
					rx={4}
					className="ghost"
				/>
			)}
			<line x1={(LIFT.left + LIFT.right) / 2} x2={(LIFT.left + LIFT.right) / 2} y1={LIFT.top} y2={y} className="part-cable" />
			<rect x={LIFT.left + 2} y={y} width={LIFT.right - LIFT.left - 4} height={LIFT.carriage} rx={4} className="part-carriage" />
			{payload && <rect x={LIFT.left + 26} y={y - 20} width={28} height={20} rx={4} className="part-payload" />}
			<text x={LIFT.right + 20} y={y + LIFT.carriage / 2 + 4} className="mech-readout">
				{position !== null ? `${h.toFixed(3)} m` : ""}
			</text>
		</>
	);
}

// Arm ----------------------------------------------------------------------------------

const ARM = { cx: 130, cy: 150, length: 110 };

function ArmShape({ radians, className, payload }: { radians: number; className: string; payload?: boolean }) {
	const x = ARM.cx + ARM.length * Math.cos(radians);
	const y = ARM.cy - ARM.length * Math.sin(radians);
	return (
		<g className={className}>
			<line x1={ARM.cx} y1={ARM.cy} x2={x} y2={y} strokeWidth={16} strokeLinecap="round" />
			{payload && <circle cx={x} cy={y} r={14} className="part-payload" />}
		</g>
	);
}

function Arm({ position, goal, payload }: Props) {
	const stop = (degrees: number) => {
		const a = (degrees * Math.PI) / 180;
		return {
			x: ARM.cx + (ARM.length + 18) * Math.cos(a),
			y: ARM.cy - (ARM.length + 18) * Math.sin(a),
		};
	};
	const low = stop(-45);
	const high = stop(135);
	return (
		<>
			<line x1={ARM.cx} y1={ARM.cy} x2={low.x} y2={low.y} className="part-stop" />
			<line x1={ARM.cx} y1={ARM.cy} x2={high.x} y2={high.y} className="part-stop" />
			<line x1={ARM.cx - 40} x2={ARM.cx + 40} y1={ARM.cy + 60} y2={ARM.cy + 60} className="part-ground" />
			<rect x={ARM.cx - 10} y={ARM.cy} width={20} height={60} className="part-rail" />
			{[0, 90].map((m) => {
				const p = stop(m);
				return (
					<text key={m} x={p.x + (m === 0 ? 6 : 0)} y={p.y + (m === 0 ? 4 : -4)} textAnchor={m === 0 ? "start" : "middle"} className="mech-label">
						{m}°
					</text>
				);
			})}
			{goal !== null && <ArmShape radians={goal} className="ghost-arm" />}
			{position !== null && <ArmShape radians={position} className="part-arm" payload={payload} />}
			<circle cx={ARM.cx} cy={ARM.cy} r={9} className="part-hub" />
			<text x={8} y={HEIGHT - 6} className="mech-readout">
				{position !== null ? `${((position * 180) / Math.PI).toFixed(1)}°` : ""}
			</text>
		</>
	);
}
