import type { Pose, Tree } from "../lab";

const FIELD_WIDTH = 9;
const FIELD_HEIGHT = 6;
const SCALE = 60;
const ROBOT = 0.8;

const px = (x: number) => x * SCALE;
const py = (y: number) => (FIELD_HEIGHT - y) * SCALE;

function isPose(value: unknown): value is Pose {
	return typeof value === "object" && value !== null && "x" in value && "y" in value;
}

function RobotMark({ pose }: { pose: Pose }) {
	const size = ROBOT * SCALE;
	const degrees = (-pose.r * 180) / Math.PI;
	return (
		<g transform={`translate(${px(pose.x)} ${py(pose.y)}) rotate(${degrees})`}>
			<rect x={-size / 2} y={-size / 2} width={size} height={size} rx={6} className="field-robot" />
			<path d={`M${size / 2 - 3},0 L${size / 8},${-size / 4} L${size / 8},${size / 4} Z`} className="field-nose" />
		</g>
	);
}

/**
 * The field, with only what the student's code logged on it. Nothing's drawn
 * until they've logged it: that's the point of the challenge.
 */
export function FieldView({ tree }: { tree: Tree | undefined }) {
	const find = (key: string) => tree?.items.find((item) => item.key === key)?.value;
	const robot = find("Odometry/Robot");
	const waypoints = find("Odometry/Waypoints");
	const trail = find("Odometry/Trail");
	const nothing = !isPose(robot) && !Array.isArray(waypoints);

	return (
		<section className="card panel">
			<span className="panel-label">Field: only what you log</span>
			<div className="field-wrap">
				<svg
					viewBox={`0 0 ${FIELD_WIDTH * SCALE} ${FIELD_HEIGHT * SCALE}`}
					role="img"
					aria-label="The poses your code logged, drawn on the field"
					className="field"
				>
					<rect width={FIELD_WIDTH * SCALE} height={FIELD_HEIGHT * SCALE} className="field-floor" />
					{Array.from({ length: FIELD_WIDTH - 1 }, (_, i) => (
						<line key={`v${i}`} x1={px(i + 1)} x2={px(i + 1)} y1={0} y2={FIELD_HEIGHT * SCALE} className="field-grid" />
					))}
					{Array.from({ length: FIELD_HEIGHT - 1 }, (_, i) => (
						<line key={`h${i}`} x1={0} x2={FIELD_WIDTH * SCALE} y1={py(i + 1)} y2={py(i + 1)} className="field-grid" />
					))}
					{Array.isArray(trail) && (
						<polyline
							points={(trail as unknown[])
								.filter(isPose)
								.map((pose) => `${px(pose.x)},${py(pose.y)}`)
								.join(" ")}
							className="field-trail"
						/>
					)}
					{Array.isArray(waypoints) &&
						(waypoints as unknown[]).filter(isPose).map((pose, index) => (
							<g key={`${pose.x},${pose.y}`}>
								<circle cx={px(pose.x)} cy={py(pose.y)} r={13} className="field-waypoint" />
								<text x={px(pose.x)} y={py(pose.y) + 4} textAnchor="middle" className="field-waypoint-text">
									{index + 1}
								</text>
							</g>
						))}
					{isPose(robot) && <RobotMark pose={robot} />}
				</svg>
				{nothing && (
					<div className="field-empty">
						The field is empty because the log is. Log <code>Odometry/Robot</code>, and the robot appears.
					</div>
				)}
			</div>
		</section>
	);
}
