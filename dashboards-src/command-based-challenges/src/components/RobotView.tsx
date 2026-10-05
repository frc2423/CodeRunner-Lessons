import { useRef } from "react";
import { hex, ledOff } from "../field";
import type { RobotState } from "../lab";

const GROUND = 205;
/** The path a piece takes through the robot, from the intake to the shooter. */
const PATH: [number, number][] = [
	[300, 158],
	[248, 146],
	[200, 112],
	[166, 78],
];
const SHOOTER_ENTRY = 1.4;
const BEAM_BREAK = 0.9;
const CLIMBER_TOP = 0.6;

function along(fraction: number): [number, number] {
	const lengths = PATH.slice(1).map((p, i) => Math.hypot(p[0] - PATH[i][0], p[1] - PATH[i][1]));
	const total = lengths.reduce((a, b) => a + b, 0);
	let left = Math.max(0, Math.min(1, fraction)) * total;
	for (let i = 0; i < lengths.length; i++) {
		if (left <= lengths[i] || i === lengths.length - 1) {
			const t = lengths[i] === 0 ? 0 : Math.min(left / lengths[i], 1);
			return [PATH[i][0] + (PATH[i + 1][0] - PATH[i][0]) * t, PATH[i][1] + (PATH[i + 1][1] - PATH[i][1]) * t];
		}
		left -= lengths[i];
	}
	return PATH[PATH.length - 1];
}

/** Spinning things keep their angle between renders. */
function useSpin() {
	const state = useRef({ last: performance.now(), roller: 0, wheel: 0, belt: 0 });
	return (rollers: number, rpm: number, indexer: number) => {
		const s = state.current;
		const now = performance.now();
		const dt = Math.min((now - s.last) / 1000, 0.1);
		s.last = now;
		s.roller = (s.roller + rollers * 900 * dt) % 360;
		s.wheel = (s.wheel + (rpm / 60) * 360 * dt * 0.15) % 360;
		s.belt = (s.belt - indexer * 60 * dt) % 1000;
		return s;
	};
}

/**
 * The robot from the side, front (intake) to the right: the intake swinging
 * out, a piece travelling through the indexer past the beam break, the shooter
 * wheel spinning up, the climber hook, and the LEDs.
 */
export function RobotView({ state }: { state: RobotState | undefined }) {
	const spin = useSpin();
	const s = state;
	const deploy = s?.deploy ?? 0;
	const rollers = s?.rollers ?? 0;
	const indexer = s?.indexer ?? 0;
	const rpm = s?.rpm ?? 0;
	const target = s?.targetRpm ?? 0;
	const piece = s?.piece ?? -1;
	const height = s?.climberHeight ?? 0;
	const hang = s?.hang ?? 0;
	const led = s && !ledOff(s.led) ? hex(s.led) : undefined;
	const angles = spin(rollers, rpm, indexer);

	const lift = hang * 55;
	// The intake arm: up inside the robot when stowed, down to the floor when deployed.
	const armAngle = (-100 + deploy * 128) * (Math.PI / 180);
	const pivot: [number, number] = [292, 136];
	const roller: [number, number] = [pivot[0] + Math.cos(armAngle) * 70, pivot[1] + Math.sin(armAngle) * 70];
	const beam = along(BEAM_BREAK / SHOOTER_ENTRY);
	const piecePoint = piece >= 0 ? along(Math.max(piece, 0) / SHOOTER_ENTRY) : null;
	const hookY = 150 - (height / CLIMBER_TOP) * 110;
	const gauge = Math.min(rpm / 4000, 1);
	const targetMark = Math.min(target / 4000, 1);

	return (
		<svg className="robot-side" viewBox="0 0 400 230" role="img" aria-label="The robot from the side">
			<line className="side-ground" x1={0} y1={GROUND} x2={400} y2={GROUND} />
			{(s?.hooked || hang > 0) && <line className="side-bar" x1={70} y1={hookY - 6} x2={150} y2={hookY - 6} />}

			<g transform={`translate(0 ${-lift})`}>
				{/* Climber tube and hook. */}
				<rect className="side-tube" x={104} y={34} width={12} height={130} rx={3} />
				<g transform={`translate(110 ${hookY})`}>
					<path className="side-hook" d="M 0 0 L 0 -16 Q 0 -26 -10 -26 Q -18 -26 -18 -18" />
				</g>

				{/* Chassis, bumpers, LEDs, wheels. */}
				<rect className="side-frame" x={90} y={120} width={220} height={52} rx={6} />
				<rect className="side-bumper" x={84} y={166} width={232} height={22} rx={8} />
				<rect
					className="side-leds"
					x={96}
					y={170}
					width={208}
					height={6}
					rx={3}
					style={led ? { fill: led, filter: `drop-shadow(0 0 5px ${led})` } : undefined}
				/>
				{[124, 276].map((x) => (
					<circle key={x} className="side-wheel" cx={x} cy={GROUND - 12} r={12} />
				))}

				{/* Indexer belts. */}
				<polyline
					className="side-belt"
					points={PATH.map((p) => p.join(",")).join(" ")}
					style={{ strokeDashoffset: angles.belt }}
				/>
				{/* Beam break. */}
				<line
					className={s?.beamBreak ? "side-beam side-beam-broken" : "side-beam"}
					x1={beam[0] - 12}
					y1={beam[1] - 14}
					x2={beam[0] + 12}
					y2={beam[1] + 14}
				/>
				<circle className="side-sensor" cx={beam[0] - 12} cy={beam[1] - 14} r={3.5} />
				<circle className="side-sensor" cx={beam[0] + 12} cy={beam[1] + 14} r={3.5} />

				{/* Shooter wheel. */}
				<g transform={`translate(150 66) rotate(${angles.wheel})`}>
					<circle className={s?.atSpeed ? "side-wheel side-flywheel-ready" : "side-wheel side-flywheel"} r={24} />
					{[0, 60, 120].map((a) => (
						<line key={a} className="side-spoke" x1={-20} y1={0} x2={20} y2={0} transform={`rotate(${a})`} />
					))}
				</g>

				{/* The piece inside the robot. */}
				{piecePoint && (
					<circle
						className={s?.jammed ? "piece piece-jammed" : "piece"}
						cx={piecePoint[0]}
						cy={piecePoint[1]}
						r={11}
					/>
				)}

				{/* Intake arm and roller. */}
				<line className="side-arm" x1={pivot[0]} y1={pivot[1]} x2={roller[0]} y2={roller[1]} />
				<g transform={`translate(${roller[0]} ${roller[1]}) rotate(${angles.roller})`}>
					<circle className={rollers !== 0 ? "side-roller side-roller-on" : "side-roller"} r={11} />
					<line className="side-spoke" x1={-9} y1={0} x2={9} y2={0} />
				</g>
			</g>

			{/* Readouts. */}
			<text className="side-label" x={150} y={20} textAnchor="middle">
				Shooter
			</text>
			<rect className="side-gauge" x={182} y={10} width={110} height={10} rx={5} />
			<rect className={s?.atSpeed ? "side-gauge-fill side-gauge-ready" : "side-gauge-fill"} x={182} y={10} width={110 * gauge} height={10} rx={5} />
			{target > 0 && <line className="side-gauge-target" x1={182 + 110 * targetMark} y1={6} x2={182 + 110 * targetMark} y2={24} />}
			<text className="side-readout" x={298} y={20}>
				{Math.round(rpm)}
				{target > 0 ? ` / ${Math.round(target)}` : ""} RPM
			</text>
			<text className="side-label" x={14} y={60}>
				Climber
			</text>
			<text className="side-readout" x={14} y={76}>
				{height.toFixed(2)} m
			</text>
			{hang > 0.98 && (
				<text className="side-readout side-good" x={14} y={94}>
					Hanging!
				</text>
			)}
			{(s?.strain ?? 0) > 0.1 && (
				<text className="side-readout side-bad" x={14} y={94}>
					Hard stop!
				</text>
			)}
			<text className="side-label" x={372} y={110} textAnchor="end">
				Intake
			</text>
			<text className="side-readout" x={372} y={126} textAnchor="end">
				{deploy > 0.9 ? "out" : deploy < 0.1 ? "in" : "moving"} · {rollers.toFixed(1)}
			</text>
			<text className="side-label" x={226} y={226} textAnchor="middle">
				{s?.jammed
					? "Jammed!"
					: s?.beamBreak
						? "Beam break: piece ready"
						: piece >= 0
							? "Piece in the robot"
							: "No piece"}
			</text>
		</svg>
	);
}
