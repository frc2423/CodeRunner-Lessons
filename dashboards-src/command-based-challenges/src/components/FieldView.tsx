import { FIELD, hex, ledOff } from "../field";
import type { RobotState, Shot } from "../lab";

/** 100 SVG units per meter; SVG y grows downward, field y grows upward. */
const S = 100;
const W = FIELD.length * S;
const H = FIELD.width * S;
const sx = (x: number) => x * S;
const sy = (y: number) => (FIELD.width - y) * S;

type Props = {
	state: RobotState | undefined;
	pieces: [number, number][];
	shots: Shot[];
	path: [number, number][];
	/** Where the robot starts in the selected challenge, drawn before anything runs. */
	start?: { x: number; y: number; heading: number };
};

/**
 * The blue half of the field from above: the hub, the tower, game pieces, the
 * robot (with its LEDs, intake and any piece it holds), shots in flight and
 * the PathPlanner path it's following.
 */
export function FieldView({ state, pieces, shots, path, start }: Props) {
	const now = state?.time ?? 0;
	const robot = state ?? (start ? restingRobot(start) : undefined);
	const odomOff =
		state && Math.hypot(state.odomX - state.x, state.odomY - state.y) + Math.abs(angleDiff(state.odomHeading, state.heading)) > 0.08;

	return (
		<svg className="field" viewBox={`-20 -20 ${W + 40} ${H + 40}`} role="img" aria-label="The field from above">
			<rect className="field-carpet" x={0} y={0} width={W} height={H} rx={8} />
			{/* Grid every meter. */}
			{Array.from({ length: Math.floor(FIELD.length) }, (_, i) => (
				<line key={`gx${i}`} className="field-grid" x1={sx(i + 1)} y1={0} x2={sx(i + 1)} y2={H} />
			))}
			{Array.from({ length: Math.floor(FIELD.width) }, (_, i) => (
				<line key={`gy${i}`} className="field-grid" x1={0} y1={sy(i + 1)} x2={W} y2={sy(i + 1)} />
			))}
			<line className="field-wall-blue" x1={0} y1={0} x2={0} y2={H} />
			<line className="field-center" x1={W} y1={0} x2={W} y2={H} />
			<text className="field-label" transform={`translate(26 ${H / 2}) rotate(-90)`} textAnchor="middle">
				Blue driver station
			</text>
			<text className="field-label" x={W - 10} y={H - 10} textAnchor="end">
				center line →
			</text>

			{/* Human player station. */}
			<rect
				className="field-zone"
				x={sx(FIELD.humanPlayer.x - 0.5)}
				y={sy(FIELD.humanPlayer.y + 0.5)}
				width={S * 1.4}
				height={S * 1.4}
				rx={6}
			/>
			<text className="field-label" x={sx(0.25)} y={sy(1.85)}>
				Human player
			</text>

			{/* Tower and its climbing zone. */}
			<circle className="field-zone" cx={sx(FIELD.tower.x)} cy={sy(FIELD.tower.y)} r={FIELD.towerZoneRadius * S} />
			<line
				className="field-bar"
				x1={sx(FIELD.tower.x - 0.5)}
				y1={sy(FIELD.tower.y)}
				x2={sx(FIELD.tower.x + 0.5)}
				y2={sy(FIELD.tower.y)}
			/>
			<text className="field-label" x={sx(FIELD.tower.x)} y={sy(FIELD.tower.y + 0.95)} textAnchor="middle">
				Tower
			</text>

			{/* The shooting spot. */}
			<rect
				className="field-spot"
				x={sx(FIELD.shootingSpot.x) - (FIELD.robotSize * S) / 2}
				y={sy(FIELD.shootingSpot.y) - (FIELD.robotSize * S) / 2}
				width={FIELD.robotSize * S}
				height={FIELD.robotSize * S}
				rx={6}
			/>

			{/* The hub. */}
			<circle className="hub-ring" cx={sx(FIELD.hub.x)} cy={sy(FIELD.hub.y)} r={FIELD.hubScoringRadius * S} />
			<circle className="hub" cx={sx(FIELD.hub.x)} cy={sy(FIELD.hub.y)} r={FIELD.hubRadius * S} />
			<text className="hub-score" x={sx(FIELD.hub.x)} y={sy(FIELD.hub.y) + 10} textAnchor="middle">
				{state ? state.score : 0}
			</text>

			{/* The path PathPlanner is following. */}
			{path.length > 1 && (
				<polyline className="field-path" points={path.map(([x, y]) => `${sx(x)},${sy(y)}`).join(" ")} />
			)}

			{/* Game pieces on the floor. */}
			{pieces.map(([x, y], i) => (
				<circle key={`p${i}-${x.toFixed(2)}-${y.toFixed(2)}`} className="piece" cx={sx(x)} cy={sy(y)} r={FIELD.pieceRadius * S} />
			))}

			{/* Where the robot's odometry thinks it is, when that's somewhere else. */}
			{state && odomOff && (
				<g transform={`translate(${sx(state.odomX)} ${sy(state.odomY)}) rotate(${(-state.odomHeading * 180) / Math.PI})`}>
					<rect
						className="robot-ghost"
						x={(-FIELD.robotSize * S) / 2}
						y={(-FIELD.robotSize * S) / 2}
						width={FIELD.robotSize * S}
						height={FIELD.robotSize * S}
						rx={6}
					/>
					<text className="field-label" y={4} textAnchor="middle">
						odometry
					</text>
				</g>
			)}

			{robot && <RobotTop robot={robot} />}

			{/* Shots in flight, and where the last ones landed. */}
			{shots.map((shot) => {
				const t = (now - shot.launched) / shot.flight;
				if (t < 0 || t > 1.6) return null;
				const p = Math.min(t, 1);
				const x = shot.x0 + (shot.x1 - shot.x0) * p;
				const y = shot.y0 + (shot.y1 - shot.y0) * p;
				const lift = shot.outcome === -1 ? 0 : Math.sin(Math.PI * p) * 0.35 * Math.hypot(shot.x1 - shot.x0, shot.y1 - shot.y0);
				return (
					<g key={`s${shot.id}`}>
						{t <= 1 && (
							<>
								<line className="shot-trail" x1={sx(shot.x0)} y1={sy(shot.y0)} x2={sx(x)} y2={sy(y)} />
								<circle className="shot-shadow" cx={sx(x)} cy={sy(y)} r={FIELD.pieceRadius * S} />
								<circle className="piece" cx={sx(x)} cy={sy(y) - lift * S * 0.4} r={FIELD.pieceRadius * S * (1 + lift * 0.6)} />
							</>
						)}
						{t > 1 && shot.outcome === 1 && (
							<text className="shot-result shot-good" x={sx(FIELD.hub.x)} y={sy(FIELD.hub.y) - 70 - (t - 1) * 60} textAnchor="middle">
								+1
							</text>
						)}
						{t > 1 && shot.outcome === 0 && (
							<text className="shot-result shot-bad" x={sx(shot.x1)} y={sy(shot.y1) - 18} textAnchor="middle">
								miss
							</text>
						)}
						{t > 1 && shot.outcome === -1 && (
							<text className="shot-result shot-bad" x={sx(shot.x1)} y={sy(shot.y1) - 18} textAnchor="middle">
								dribbled
							</text>
						)}
					</g>
				);
			})}
		</svg>
	);
}

function RobotTop({ robot }: { robot: RobotState }) {
	const half = (FIELD.robotSize * S) / 2;
	const led = ledOff(robot.led) ? undefined : hex(robot.led);
	const intakeReach = 18 + robot.deploy * 22;
	const hasPiece = robot.piece >= 0;
	// The piece moves from the intake (front) toward the shooter (middle of the robot).
	const pieceX = half - 8 - Math.max(0, Math.min(robot.piece, 1.4)) * (half / 1.4);
	return (
		<g
			className={robot.hang > 0 ? "robot robot-hanging" : "robot"}
			transform={`translate(${sx(robot.x)} ${sy(robot.y)}) rotate(${(-robot.heading * 180) / Math.PI})`}
		>
			{/* The intake swings out past the front bumper. */}
			<rect
				className={robot.rollers > 0.1 ? "robot-intake robot-intake-on" : "robot-intake"}
				x={half - 20}
				y={-28}
				width={intakeReach}
				height={56}
				rx={4}
			/>
			<rect className="robot-body" x={-half} y={-half} width={half * 2} height={half * 2} rx={8} />
			<rect
				className="robot-bumper"
				x={-half}
				y={-half}
				width={half * 2}
				height={half * 2}
				rx={8}
				style={led ? { stroke: led, filter: `drop-shadow(0 0 6px ${led})` } : undefined}
			/>
			{/* Front arrow. */}
			<path className="robot-arrow" d={`M ${half - 26} 0 L ${half - 44} -12 L ${half - 44} 12 Z`} />
			{/* Shooter wheel (from above). */}
			<rect
				className={robot.atSpeed ? "robot-shooter robot-shooter-ready" : "robot-shooter"}
				x={-14}
				y={-24}
				width={12}
				height={48}
				rx={4}
			/>
			{hasPiece && (
				<circle
					className={robot.jammed ? "piece piece-jammed" : "piece"}
					cx={pieceX}
					cy={0}
					r={FIELD.pieceRadius * S}
				/>
			)}
		</g>
	);
}

function restingRobot(start: { x: number; y: number; heading: number }): RobotState {
	return {
		time: 0,
		x: start.x,
		y: start.y,
		heading: start.heading,
		vx: 0,
		vy: 0,
		omega: 0,
		deploy: 0,
		rollers: 0,
		indexer: 0,
		piece: -1,
		rpm: 0,
		targetRpm: 0,
		climberHeight: 0,
		climberVolts: 0,
		hang: 0,
		led: [0, 0, 0],
		score: 0,
		misses: 0,
		dribbles: 0,
		enabled: false,
		jammed: false,
		rollerCurrent: 0,
		beamBreak: false,
		hooked: false,
		odomX: start.x,
		odomY: start.y,
		odomHeading: start.heading,
		strain: 0,
		atSpeed: false,
		run: 0,
	};
}

function angleDiff(a: number, b: number): number {
	let d = a - b;
	while (d > Math.PI) d -= 2 * Math.PI;
	while (d < -Math.PI) d += 2 * Math.PI;
	return d;
}
