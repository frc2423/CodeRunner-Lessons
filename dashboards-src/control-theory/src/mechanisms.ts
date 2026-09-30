import type { MechanismKind } from "./lab";

/**
 * How the dashboard shows each mechanism's numbers. The robot works in its
 * own units (see Mechanism.java); `display` converts to what a student reads,
 * which differs only for the arm (radians on the robot, degrees here).
 */
export type MechanismInfo = {
	name: string;
	/** What the goal and output are, e.g. "Speed". */
	quantity: string;
	unit: string;
	decimals: number;
	display: (value: number) => number;
	/** Inverse of `display`. */
	robot: (value: number) => number;
	/** Sandbox goal range, in display units. */
	goalMin: number;
	goalMax: number;
	goalStep: number;
	/** Plot range to start from, in display units. */
	plotMin: number;
	plotMax: number;
	events: { id: "shot" | "payload" | "bump"; label: string }[];
};

const identity = (value: number) => value;
const DEG = 180 / Math.PI;

export const MECHANISMS: Record<MechanismKind, MechanismInfo> = {
	flywheel: {
		name: "Shooter flywheel",
		quantity: "Speed",
		unit: "RPM",
		decimals: 0,
		display: identity,
		robot: identity,
		goalMin: 0,
		goalMax: 5500,
		goalStep: 50,
		plotMin: 0,
		plotMax: 5000,
		events: [{ id: "shot", label: "Shoot a ball" }],
	},
	steering: {
		name: "Swerve module steering",
		quantity: "Angle",
		unit: "°",
		decimals: 1,
		display: identity,
		robot: identity,
		goalMin: -180,
		goalMax: 180,
		goalStep: 1,
		plotMin: -180,
		plotMax: 180,
		events: [{ id: "bump", label: "Bump the wheel" }],
	},
	elevator: {
		name: "Elevator",
		quantity: "Height",
		unit: "m",
		decimals: 3,
		display: identity,
		robot: identity,
		goalMin: 0,
		goalMax: 1.6,
		goalStep: 0.01,
		plotMin: 0,
		plotMax: 1.6,
		events: [
			{ id: "payload", label: "Grab / drop a game piece" },
			{ id: "bump", label: "Bump the carriage" },
		],
	},
	arm: {
		name: "Arm",
		quantity: "Angle",
		unit: "°",
		decimals: 1,
		display: (radians) => radians * DEG,
		robot: (degrees) => degrees / DEG,
		goalMin: -45,
		goalMax: 135,
		goalStep: 1,
		plotMin: -45,
		plotMax: 135,
		events: [
			{ id: "payload", label: "Grab / drop a game piece" },
			{ id: "bump", label: "Bump the arm" },
		],
	},
};

export function formatValue(info: MechanismInfo, robotValue: number): string {
	const value = info.display(robotValue);
	return `${value.toFixed(info.decimals)}${info.unit === "°" ? "°" : ` ${info.unit}`}`;
}
