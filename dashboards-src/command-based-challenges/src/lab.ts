import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNTConnection, useNTValue } from "./coderunner/hooks";

/**
 * The NetworkTables protocol shared with the lesson's `CommandLab.java`, and
 * the hooks the dashboard reads the lab through.
 */

export type RunMode = "trial" | "free";
export type Phase = "" | "waiting" | "running" | "done";

// Dashboard → robot (SmartDashboard table) --------------------------------------

/** `"<challenge id>|<trial|free>#<nonce>"`, or `"#<nonce>"` to stop. */
export const RUN_REQUEST = "/SmartDashboard/CommandLab/RunRequest";
/** The on-screen controller: `[button bits, d-pad angle, six axes]`. */
export const VIRTUAL = "/SmartDashboard/CommandLab/Virtual";
/** `"rollin#<nonce>"` or `"reset#<nonce>"`. */
export const FIELD_EVENT = "/SmartDashboard/CommandLab/FieldEvent";

// Robot → dashboard -------------------------------------------------------------

const OUT = "/AdvantageKit/RealOutputs/CommandLab";
export const RUNNING = `${OUT}/Running`;
export const MODE = `${OUT}/Mode`;
export const PHASE = `${OUT}/Phase`;
export const WAITING = `${OUT}/Waiting`;
export const NOTICE = `${OUT}/Notice`;
export const RUN_ID = `${OUT}/RunId`;
export const ERROR = `${OUT}/Error`;
export const TIME = `${OUT}/Time`;
export const DURATION = `${OUT}/Duration`;
export const STATE = `${OUT}/State`;
export const PIECES = `${OUT}/Pieces`;
export const SHOTS = `${OUT}/Shots`;
export const CONTROLLER = `${OUT}/Controller`;
export const COMMANDS = `${OUT}/Commands`;
export const SUBSYSTEMS = `${OUT}/Subsystems`;
export const EVENTS = `${OUT}/Events`;
export const PATH = `${OUT}/Path`;
/** Plain NetworkTables topics, JSON strings. */
export const CATALOG = "/CommandLab/Catalog";
export const RESULT = "/CommandLab/Result";

export type CatalogEntry = {
	id: string;
	mode: "teleop" | "auto";
	duration: number;
	driverControlled: boolean;
	checks: { label: string; bonus: boolean }[];
};

export type CheckResult = {
	label: string;
	bonus: boolean;
	pass: boolean;
	detail: string;
};

export type TrialResult = {
	id: string;
	runId: string;
	passed: boolean;
	stars: number;
	score: number;
	checks: CheckResult[];
};

/** The simulated robot, unpacked from `CommandLab/State`. */
export type RobotState = {
	time: number;
	x: number;
	y: number;
	heading: number;
	vx: number;
	vy: number;
	omega: number;
	/** 0 stowed, 1 deployed. */
	deploy: number;
	rollers: number;
	indexer: number;
	/** How far the piece has travelled through the robot, or -1 if empty. */
	piece: number;
	rpm: number;
	targetRpm: number;
	climberHeight: number;
	climberVolts: number;
	/** 0 on the floor, 1 hanging. */
	hang: number;
	led: [number, number, number];
	score: number;
	misses: number;
	dribbles: number;
	enabled: boolean;
	jammed: boolean;
	rollerCurrent: number;
	beamBreak: boolean;
	hooked: boolean;
	odomX: number;
	odomY: number;
	odomHeading: number;
	strain: number;
	atSpeed: boolean;
	/** Goes up by one every time the field is reset. */
	run: number;
};

export function toState(v: number[] | undefined): RobotState | undefined {
	if (!Array.isArray(v) || v.length < 33) return undefined;
	return {
		time: v[0],
		x: v[1],
		y: v[2],
		heading: v[3],
		vx: v[4],
		vy: v[5],
		omega: v[6],
		deploy: v[7],
		rollers: v[8],
		indexer: v[9],
		piece: v[10],
		rpm: v[11],
		targetRpm: v[12],
		climberHeight: v[13],
		climberVolts: v[14],
		hang: v[15],
		led: [v[16], v[17], v[18]],
		score: v[19],
		misses: v[20],
		dribbles: v[21],
		enabled: v[22] > 0.5,
		jammed: v[23] > 0.5,
		rollerCurrent: v[24],
		beamBreak: v[25] > 0.5,
		hooked: v[26] > 0.5,
		odomX: v[27],
		odomY: v[28],
		odomHeading: v[29],
		strain: v[30],
		atSpeed: v[31] > 0.5,
		run: v[32],
	};
}

export type Shot = {
	id: number;
	x0: number;
	y0: number;
	x1: number;
	y1: number;
	launched: number;
	flight: number;
	/** 1 scored, 0 missed, -1 dribbled out. */
	outcome: number;
	rpm: number;
};

function toShots(v: number[] | undefined): Shot[] {
	const shots: Shot[] = [];
	if (!Array.isArray(v)) return shots;
	for (let i = 0; i + 9 <= v.length; i += 9) {
		shots.push({
			id: v[i],
			x0: v[i + 1],
			y0: v[i + 2],
			x1: v[i + 3],
			y1: v[i + 4],
			launched: v[i + 5],
			flight: v[i + 6],
			outcome: v[i + 7],
			rpm: v[i + 8],
		});
	}
	return shots;
}

function toPoints(v: number[] | undefined): [number, number][] {
	const points: [number, number][] = [];
	if (!Array.isArray(v)) return points;
	for (let i = 0; i + 2 <= v.length; i += 2) points.push([v[i], v[i + 1]]);
	return points;
}

/** The controller as the robot sees it. */
export type ControllerState = {
	/** "script" while a trial's scripted driver has the controller. */
	source: "real" | "script";
	buttons: number;
	pov: number;
	axes: number[];
	rumble: number;
};

function toController(v: number[] | undefined): ControllerState {
	if (!Array.isArray(v) || v.length < 10) {
		return { source: "real", buttons: 0, pov: -1, axes: [0, 0, 0, 0, 0, 0], rumble: 0 };
	}
	return {
		source: v[0] === 2 ? "script" : "real",
		buttons: v[1],
		pov: v[2],
		axes: v.slice(3, 9),
		rumble: v[9],
	};
}

export type SubsystemInfo = { name: string; current: string; fallback: string };

function toSubsystems(v: string[] | undefined): SubsystemInfo[] {
	if (!Array.isArray(v)) return [];
	return v.map((line) => {
		const [name, current = "", fallback = ""] = line.split("\t");
		return { name, current, fallback };
	});
}

export type CommandEvent = {
	seq: number;
	run: number;
	time: number;
	type: "start" | "finish" | "interrupt";
	name: string;
	requirements: string[];
	by: string;
};

function toEvents(v: string[] | undefined): CommandEvent[] {
	if (!Array.isArray(v)) return [];
	return v.map((line) => {
		const [seq, run, time, type, name, reqs = "", by = ""] = line.split("\t");
		return {
			seq: Number(seq),
			run: Number(run),
			time: Number(time),
			type: type as CommandEvent["type"],
			name,
			requirements: reqs ? reqs.split(",") : [],
			by,
		};
	});
}

const noop = () => {};

function parseJson<T>(text: string | undefined): T | undefined {
	if (!text) return undefined;
	try {
		return JSON.parse(text) as T;
	} catch {
		return undefined;
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

// The command timeline ------------------------------------------------------------

/** One command's run on the timeline. `end` is null while it's still running. */
export type Bar = {
	key: string;
	name: string;
	requirements: string[];
	start: number;
	end: number | null;
	interrupted: boolean;
	by: string;
};

/** Button presses over time, for the controller lane. */
export type InputSample = { time: number; buttons: number; pov: number; lt: number; rt: number };

export type Timeline = { bars: Bar[]; inputs: InputSample[] };

const KEEP_SECONDS = 70;

/**
 * Pairs command start/end events into bars, and keeps button presses, for the
 * current run only: every reset of the field starts a new timeline.
 */
function useTimeline(): Timeline {
	const state = useRef({
		lastSeq: 0,
		run: -1,
		time: 0,
		bars: [] as (Bar & { run: number })[],
		inputs: [] as InputSample[],
	});
	const [timeline, setTimeline] = useState<Timeline>({ bars: [], inputs: [] });
	const frame = useRef<number | null>(null);

	useEffect(() => {
		const flush = () => {
			frame.current = null;
			const s = state.current;
			setTimeline({ bars: s.bars.filter((bar) => bar.run === s.run), inputs: s.inputs.slice() });
		};
		const schedule = () => {
			if (frame.current === null) frame.current = window.requestAnimationFrame(flush);
		};

		const unsubscribe =
			window.coderunner?.nt.subscribe<number[] | string[]>(
				[STATE, EVENTS, CONTROLLER],
				(value, entry) => {
					const s = state.current;
					if (entry.topic === STATE) {
						const robot = toState(value as number[]);
						if (!robot) return;
						if (robot.run !== s.run) {
							s.run = robot.run;
							s.inputs = [];
							s.bars = s.bars.filter((bar) => bar.run === robot.run);
						}
						s.time = robot.time;
						schedule();
						return;
					}
					if (entry.topic === CONTROLLER) {
						const c = toController(value as number[]);
						const last = s.inputs[s.inputs.length - 1];
						if (last && last.time >= s.time) return;
						s.inputs.push({ time: s.time, buttons: c.buttons, pov: c.pov, lt: c.axes[2], rt: c.axes[3] });
						while (s.inputs.length > 0 && s.inputs[0].time < s.time - KEEP_SECONDS) s.inputs.shift();
						schedule();
						return;
					}
					for (const event of toEvents(value as string[])) {
						if (event.seq <= s.lastSeq) continue;
						s.lastSeq = event.seq;
						const key = `${event.name}|${event.requirements.join(",")}`;
						if (event.type === "start") {
							s.bars.push({
								key: `${event.seq}`,
								run: event.run,
								name: event.name,
								requirements: event.requirements,
								start: event.time,
								end: null,
								interrupted: false,
								by: "",
							});
						} else {
							const open = s.bars.find(
								(bar) =>
									bar.end === null &&
									bar.run === event.run &&
									`${bar.name}|${bar.requirements.join(",")}` === key,
							);
							if (open) {
								open.end = event.time;
								open.interrupted = event.type === "interrupt";
								open.by = event.by;
							}
						}
					}
					s.bars = s.bars.filter((bar) => bar.end === null || bar.end > s.time - KEEP_SECONDS);
					schedule();
				},
				{ periodic: 0.02 },
			) ?? noop;
		return () => {
			unsubscribe();
			if (frame.current !== null) window.cancelAnimationFrame(frame.current);
		};
	}, []);

	return timeline;
}

/**
 * True when something is running but the robot has stopped sending updates,
 * which almost always means student code is stuck in a loop that never ends.
 */
function useRobotStalled(active: boolean): boolean {
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
		if (!active) return;
		const timer = window.setInterval(() => {
			setStalled(Date.now() - lastUpdate.current > 2000);
		}, 500);
		return () => window.clearInterval(timer);
	}, [active]);

	return stalled;
}

/** The latest result of every challenge run since the dashboard opened. */
function useResults(): Record<string, TrialResult> {
	const [results, setResults] = useState<Record<string, TrialResult>>({});
	useEffect(
		() =>
			window.coderunner?.nt.subscribe<string>(RESULT, (text) => {
				const result = parseJson<TrialResult>(text);
				if (!result?.id) return;
				setResults((previous) => ({ ...previous, [result.id]: result }));
			}) ?? noop,
		[],
	);
	return results;
}

// Everything together -------------------------------------------------------------

export type Lab = {
	connected: boolean;
	catalog: CatalogEntry[] | undefined;
	/** Id of the loaded challenge, or "". */
	running: string;
	mode: RunMode | "";
	phase: Phase;
	waiting: string;
	notice: string;
	error: string;
	stalled: boolean;
	duration: number;
	state: RobotState | undefined;
	pieces: [number, number][];
	shots: Shot[];
	path: [number, number][];
	controller: ControllerState;
	subsystems: SubsystemInfo[];
	timeline: Timeline;
	results: Record<string, TrialResult>;
	run(id: string, mode: RunMode): void;
	stop(): void;
	fieldEvent(event: "rollin" | "reset"): void;
	setVirtual(values: number[]): void;
};

export function useLab(): Lab {
	const connected = useNTConnection();
	const [catalogText] = useNTValue<string>(CATALOG);
	const [running] = useNTValue<string>(RUNNING, "");
	const [mode] = useNTValue<string>(MODE, "");
	const [phase] = useNTValue<string>(PHASE, "");
	const [waiting] = useNTValue<string>(WAITING, "");
	const [notice] = useNTValue<string>(NOTICE, "");
	const [error] = useNTValue<string>(ERROR, "");
	const [duration] = useNTValue<number>(DURATION, 0);
	const [stateValues] = useNTValue<number[]>(STATE);
	const [pieceValues] = useNTValue<number[]>(PIECES);
	const [shotValues] = useNTValue<number[]>(SHOTS);
	const [pathValues] = useNTValue<number[]>(PATH);
	const [controllerValues] = useNTValue<number[]>(CONTROLLER);
	const [subsystemValues] = useNTValue<string[]>(SUBSYSTEMS);
	const [, setRequest] = useNTValue<string>(RUN_REQUEST);
	const timeline = useTimeline();
	const results = useResults();
	const stalled = useRobotStalled(connected && running !== "");

	const catalog = useMemo(
		() => parseJson<{ challenges: CatalogEntry[] }>(catalogText)?.challenges,
		[catalogText],
	);
	const state = useMemo(() => toState(stateValues), [stateValues]);
	const pieces = useMemo(() => toPoints(pieceValues), [pieceValues]);
	const shots = useMemo(() => toShots(shotValues), [shotValues]);
	const path = useMemo(() => toPoints(pathValues), [pathValues]);
	const controller = useMemo(() => toController(controllerValues), [controllerValues]);
	const subsystems = useMemo(() => toSubsystems(subsystemValues), [subsystemValues]);

	// The last thing the student ran, so it runs again by itself after they
	// edit their code and restart the robot program.
	const lastRun = useRef<{ id: string; mode: RunMode } | null>(null);

	const run = useCallback(
		(id: string, runMode: RunMode) => {
			lastRun.current = { id, mode: runMode };
			setRequest(`${id}|${runMode}#${Date.now()}`, "string");
		},
		[setRequest],
	);
	const stop = useCallback(() => {
		lastRun.current = null;
		setRequest(`#${Date.now()}`, "string");
	}, [setRequest]);

	const wasConnected = useRef(false);
	useEffect(() => {
		const connecting = connected && !wasConnected.current;
		wasConnected.current = connected;
		if (!connecting) return;
		const last = lastRun.current;
		if (last === null) return;
		// Give the new robot program a moment to set up the lab.
		const timer = window.setTimeout(() => run(last.id, last.mode), 1000);
		return () => window.clearTimeout(timer);
	}, [connected, run]);

	const fieldEvent = useCallback((event: string) => {
		window.coderunner?.nt.setValue(FIELD_EVENT, `${event}#${Date.now()}`, "string");
	}, []);
	const setVirtual = useCallback((values: number[]) => {
		window.coderunner?.nt.setValue(VIRTUAL, values, "double[]");
	}, []);

	const live = connected;
	return {
		connected,
		catalog: live ? catalog : undefined,
		running: live ? running : "",
		mode: live && (mode === "trial" || mode === "free") ? mode : "",
		phase: live ? (phase as Phase) : "",
		waiting: live ? waiting : "",
		notice: live ? notice : "",
		error: live ? error : "",
		stalled,
		duration,
		state: live ? state : undefined,
		pieces: live ? pieces : [],
		shots: live ? shots : [],
		path: live ? path : [],
		controller,
		subsystems: live ? subsystems : [],
		timeline,
		results,
		run,
		stop,
		fieldEvent,
		setVirtual,
	};
}
