import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNTConnection, useNTValue } from "./coderunner/hooks";

/**
 * The NetworkTables protocol shared with the lesson's `Playground.java`, and
 * the hooks the dashboard reads the playground through.
 */

// Dashboard → robot (SmartDashboard table) --------------------------------------

/** The tab's keyboard, as six controller axes in XboxController.Axis order. */
export const KEYS = "/SmartDashboard/Playground/Keys";
/** Drawn obstacles, five numbers each: center X, center Y, X width, Y width, rotation (rad). */
export const DRAWN = "/SmartDashboard/Playground/DrawnObstacles";
/** Whether the robot calls SmartSteering.steer at all. */
export const SMART = "/SmartDashboard/Playground/SmartSteering";
/** `"reset#<nonce>"`. */
export const COMMAND = "/SmartDashboard/Playground/Command";

// Robot → dashboard -------------------------------------------------------------

const OUT = "/AdvantageKit/RealOutputs/Playground";
export const STATE = `${OUT}/State`;
export const OBSTACLES = `${OUT}/Obstacles`;
export const BUMPS = `${OUT}/Bumps`;
export const SHAPES = `${OUT}/Debug/Shapes`;
export const VALUES = `${OUT}/Debug/Values`;
export const ERROR = `${OUT}/Error`;
export const OBSTACLE_ERROR = `${OUT}/ObstacleError`;
export const TIME = `${OUT}/Time`;
/** Plain NetworkTables topic, a JSON string published once at startup. */
export const CONFIG = "/Playground/Config";

/** The field and robot, from PlaygroundConstants.java. */
export type Config = {
	fieldLength: number;
	fieldWidth: number;
	robotLength: number;
	robotWidth: number;
	maxSpeed: number;
	maxAngularSpeed: number;
	startX: number;
	startY: number;
	startHeading: number;
};

/** Used until the robot program sends its own. Matches PlaygroundConstants.java. */
export const DEFAULT_CONFIG: Config = {
	fieldLength: 16.541,
	fieldWidth: 8.069,
	robotLength: 0.9,
	robotWidth: 0.75,
	maxSpeed: 4.5,
	maxAngularSpeed: 1.5 * Math.PI,
	startX: 1.5,
	startY: 8.069 / 2,
	startHeading: 0,
};

/** Field-relative speeds: m/s and rad/s. */
export type Speeds = { vx: number; vy: number; omega: number };

export type RobotState = {
	time: number;
	x: number;
	y: number;
	heading: number;
	actual: Speeds;
	requested: Speeds;
	steered: Speeds;
	/** Enabled in Teleop, so the sticks drive. */
	driving: boolean;
	smartOn: boolean;
	bumps: number;
	touching: boolean;
	/** How long SmartSteering.steer took, in microseconds. */
	steerMicros: number;
	/** Goes up by one every time the robot is reset. */
	resets: number;
	contactTime: number;
	distance: number;
	hardestBump: number;
	sticks: [number, number, number];
};

function toState(v: number[] | undefined): RobotState | undefined {
	if (!Array.isArray(v) || v.length < 25) return undefined;
	return {
		time: v[0],
		x: v[1],
		y: v[2],
		heading: v[3],
		actual: { vx: v[4], vy: v[5], omega: v[6] },
		requested: { vx: v[7], vy: v[8], omega: v[9] },
		steered: { vx: v[10], vy: v[11], omega: v[12] },
		driving: v[13] > 0.5,
		smartOn: v[14] > 0.5,
		bumps: v[15],
		touching: v[16] > 0.5,
		steerMicros: v[17],
		resets: v[18],
		contactTime: v[19],
		distance: v[20],
		hardestBump: v[21],
		sticks: [v[22], v[23], v[24]],
	};
}

/** A rectangle on the field, like WPILib's Rectangle2d. */
export type Rect = { cx: number; cy: number; xw: number; yw: number; rot: number };

/** An obstacle drawn on this tab. */
export type DrawnObstacle = Rect & { id: string };

function toRects(v: number[] | undefined, stride: number): Rect[] {
	const rects: Rect[] = [];
	if (!Array.isArray(v)) return rects;
	for (let i = 0; i + stride <= v.length; i += stride) {
		// Only the obstacles from Obstacles.java: drawn ones come from this tab.
		if (stride === 6 && v[i + 5] !== 0) continue;
		rects.push({ cx: v[i], cy: v[i + 1], xw: v[i + 2], yw: v[i + 3], rot: v[i + 4] });
	}
	return rects;
}

export type Bump = { x: number; y: number; time: number; speed: number };

function toBumps(v: number[] | undefined): Bump[] {
	const bumps: Bump[] = [];
	if (!Array.isArray(v)) return bumps;
	for (let i = 0; i + 4 <= v.length; i += 4) {
		bumps.push({ x: v[i], y: v[i + 1], time: v[i + 2], speed: v[i + 3] });
	}
	return bumps;
}

export const SHAPE_COLORS = ["yellow", "red", "green", "blue", "purple", "white"] as const;
export type ShapeColor = (typeof SHAPE_COLORS)[number];

/** Something SmartSteering drew with the Debug class. */
export type Shape =
	| { kind: "point"; color: ShapeColor; x: number; y: number }
	| { kind: "line" | "arrow"; color: ShapeColor; x1: number; y1: number; x2: number; y2: number }
	| { kind: "rectangle"; color: ShapeColor; rect: Rect }
	| { kind: "circle"; color: ShapeColor; x: number; y: number; r: number };

function toShapes(v: number[] | undefined): Shape[] {
	const shapes: Shape[] = [];
	if (!Array.isArray(v)) return shapes;
	for (let i = 0; i + 7 <= v.length; i += 7) {
		const color = SHAPE_COLORS[v[i + 1]] ?? "yellow";
		const [a, b, c, d, e] = v.slice(i + 2, i + 7);
		switch (v[i]) {
			case 0:
				shapes.push({ kind: "point", color, x: a, y: b });
				break;
			case 1:
				shapes.push({ kind: "line", color, x1: a, y1: b, x2: c, y2: d });
				break;
			case 2:
				shapes.push({ kind: "rectangle", color, rect: { cx: a, cy: b, xw: c, yw: d, rot: e } });
				break;
			case 3:
				shapes.push({ kind: "circle", color, x: a, y: b, r: c });
				break;
			case 4:
				shapes.push({ kind: "arrow", color, x1: a, y1: b, x2: c, y2: d });
				break;
		}
	}
	return shapes;
}

function toValues(v: string[] | undefined): [string, string][] {
	if (!Array.isArray(v)) return [];
	return v.map((line) => {
		const tab = line.indexOf("\t");
		return tab < 0 ? [line, ""] : [line.slice(0, tab), line.slice(tab + 1)];
	});
}

function parseConfig(text: string | undefined): Config {
	if (!text) return DEFAULT_CONFIG;
	try {
		return { ...DEFAULT_CONFIG, ...(JSON.parse(text) as Partial<Config>) };
	} catch {
		return DEFAULT_CONFIG;
	}
}

// Browser storage can be missing or blocked in a sandboxed dashboard. ------------

export function loadJson<T>(key: string, fallback: T): T {
	try {
		const text = window.localStorage.getItem(key);
		return text ? (JSON.parse(text) as T) : fallback;
	} catch {
		return fallback;
	}
}

export function saveJson(key: string, value: unknown): void {
	try {
		window.localStorage.setItem(key, JSON.stringify(value));
	} catch {}
}

const noop = () => {};

/**
 * True when the robot program is connected but has stopped sending updates,
 * which almost always means SmartSteering is stuck in a loop that never ends.
 */
function useRobotStalled(connected: boolean): boolean {
	const lastUpdate = useRef(Date.now());
	const [stalled, setStalled] = useState(false);

	useEffect(
		() =>
			window.coderunner?.nt.subscribe(
				TIME,
				() => {
					lastUpdate.current = Date.now();
					setStalled(false);
				},
				{ periodic: 0.1, immediate: false },
			) ?? noop,
		[],
	);

	useEffect(() => {
		lastUpdate.current = Date.now();
		setStalled(false);
		if (!connected) return;
		const timer = window.setInterval(() => setStalled(Date.now() - lastUpdate.current > 2000), 500);
		return () => window.clearInterval(timer);
	}, [connected]);

	return stalled;
}

/**
 * Calls `send` now, whenever `value` changes, and again a moment after the
 * robot program (re)connects, so a freshly restarted program gets the tab's
 * settings without the student touching anything.
 */
function useSendToRobot<T>(value: T, connected: boolean, send: (value: T) => void): void {
	const latest = useRef(value);
	latest.current = value;
	useEffect(() => {
		send(value);
	}, [value, send]);
	useEffect(() => {
		if (!connected) return;
		const timers = [300, 1500].map((delay) => window.setTimeout(() => send(latest.current), delay));
		return () => {
			for (const timer of timers) window.clearTimeout(timer);
		};
	}, [connected, send]);
}

// Everything together -------------------------------------------------------------

export type Playground = {
	connected: boolean;
	stalled: boolean;
	config: Config;
	state: RobotState | undefined;
	codeObstacles: Rect[];
	bumps: Bump[];
	shapes: Shape[];
	values: [string, string][];
	error: string;
	obstacleError: string;
	reset(): void;
	setKeys(axes: number[]): void;
};

export function usePlayground(drawn: DrawnObstacle[], smartOn: boolean): Playground {
	const connected = useNTConnection();
	const stalled = useRobotStalled(connected);
	const [configText] = useNTValue<string>(CONFIG);
	const [stateValues] = useNTValue<number[]>(STATE);
	const [obstacleValues] = useNTValue<number[]>(OBSTACLES);
	const [bumpValues] = useNTValue<number[]>(BUMPS);
	const [shapeValues] = useNTValue<number[]>(SHAPES);
	const [valueLines] = useNTValue<string[]>(VALUES);
	const [error] = useNTValue<string>(ERROR, "");
	const [obstacleError] = useNTValue<string>(OBSTACLE_ERROR, "");

	const config = useMemo(() => parseConfig(configText), [configText]);
	const state = useMemo(() => toState(stateValues), [stateValues]);
	const codeObstacles = useMemo(() => toRects(obstacleValues, 6), [obstacleValues]);
	const bumps = useMemo(() => toBumps(bumpValues), [bumpValues]);
	const shapes = useMemo(() => toShapes(shapeValues), [shapeValues]);
	const values = useMemo(() => toValues(valueLines), [valueLines]);

	const sendDrawn = useCallback((list: DrawnObstacle[]) => {
		const flat = list.flatMap((o) => [o.cx, o.cy, o.xw, o.yw, o.rot]);
		window.coderunner?.nt.setValue(DRAWN, flat, "double[]");
	}, []);
	const sendSmart = useCallback((on: boolean) => {
		window.coderunner?.nt.setValue(SMART, on, "boolean");
	}, []);
	useSendToRobot(drawn, connected, sendDrawn);
	useSendToRobot(smartOn, connected, sendSmart);

	const reset = useCallback(() => {
		window.coderunner?.nt.setValue(COMMAND, `reset#${Date.now()}`, "string");
	}, []);
	const setKeys = useCallback((axes: number[]) => {
		window.coderunner?.nt.setValue(KEYS, axes, "double[]");
	}, []);

	const live = connected;
	return {
		connected,
		stalled: live && stalled,
		config,
		state: live ? state : undefined,
		codeObstacles: live ? codeObstacles : [],
		bumps: live ? bumps : [],
		shapes: live ? shapes : [],
		values: live ? values : [],
		error: live ? error : "",
		obstacleError: live ? obstacleError : "",
		reset,
		setKeys,
	};
}
