import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNTConnection, useNTValue } from "./coderunner/hooks";

/**
 * The NetworkTables protocol shared with the lesson's `ControlLab.java`, and
 * the hooks the dashboard reads the lab through.
 */

export type MechanismKind = "flywheel" | "steering" | "elevator" | "arm";
export type RunMode = "trial" | "sandbox";

// Dashboard → robot (SmartDashboard table) --------------------------------------

/** `"<challenge id>|<trial|sandbox>#<nonce>"`, or `"#<nonce>"` to stop. */
export const RUN_REQUEST = "/SmartDashboard/ControlLab/RunRequest";
export const SANDBOX_GOAL = "/SmartDashboard/ControlLab/Sandbox/Goal";
/** `"shot#<nonce>"`, `"payload#<nonce>"` (toggles) or `"bump#<nonce>"`. */
export const SANDBOX_EVENT = "/SmartDashboard/ControlLab/Sandbox/Event";
export const tunableTopic = (id: string, name: string) =>
	`/SmartDashboard/ControlLab/Tune/${id}/${name}`;

// Robot → dashboard -------------------------------------------------------------

const OUTPUTS = "/AdvantageKit/RealOutputs/ControlLab";
export const RUNNING = `${OUTPUTS}/Running`;
export const MODE = `${OUTPUTS}/Mode`;
export const RUN_ID = `${OUTPUTS}/RunId`;
export const ERROR = `${OUTPUTS}/Error`;
export const TIME = `${OUTPUTS}/Time`;
export const MECHANISM = `${OUTPUTS}/Mechanism`;
export const PAYLOAD = `${OUTPUTS}/Payload`;
export const SHOTS = `${OUTPUTS}/Shots`;
/**
 * `[time, goal, output, measured, volts, estimate, position, velocity, …]`,
 * then one value per line the student draws with `mechanism.plot(name, value)`.
 */
export const SAMPLE = `${OUTPUTS}/Sample`;
/** Names of the student's plot lines, in the order they follow in SAMPLE. */
export const PLOT_NAMES = `${OUTPUTS}/PlotNames`;
/** Plain NetworkTables topics, JSON strings. */
export const CATALOG = "/ControlLab/Catalog";
export const RESULT = "/ControlLab/Result";

export type CatalogEntry = {
	id: string;
	mechanism: MechanismKind;
	units: string;
	duration: number;
	/** Why making the controller failed at startup, or "". */
	problem: string;
	checks: { label: string; bonus: boolean }[];
	tunables: { name: string; default: number }[];
};

export type CheckResult = {
	label: string;
	bonus: boolean;
	pass: boolean;
	detail: string;
};

export type SysIdFit = {
	ok: boolean;
	problem: string;
	kS: number | null;
	kV: number | null;
	kA: number | null;
	samples: number;
};

/** One line of a plot; `null` where there is no value. */
export type Line = (number | null)[];

export type Trace = {
	time: number[];
	goal: number[];
	output: number[];
	measured: number[];
	volts: number[];
	estimate: Line;
	plots: Record<string, Line>;
};

export type TrialResult = {
	id: string;
	runId: string;
	checks: CheckResult[];
	passed: boolean;
	stars: number;
	fit?: SysIdFit;
	trace: Trace;
};

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

// Tunable numbers -----------------------------------------------------------------

const TUNABLES_KEY = "control-lab:tunables";
type StoredTunables = Record<string, Record<string, number>>;

/** Remembers a slider value so it survives a robot restart. */
export function rememberTunable(id: string, name: string, value: number): void {
	const stored = loadJson<StoredTunables>(TUNABLES_KEY, {});
	stored[id] = { ...stored[id], [name]: value };
	saveJson(TUNABLES_KEY, stored);
}

export function forgetTunable(id: string, name: string): void {
	const stored = loadJson<StoredTunables>(TUNABLES_KEY, {});
	if (stored[id]) delete stored[id][name];
	saveJson(TUNABLES_KEY, stored);
}

/** Sends every remembered slider value to a (re)started robot program. */
function restoreTunables(): void {
	const nt = window.coderunner?.nt;
	if (!nt) return;
	const stored = loadJson<StoredTunables>(TUNABLES_KEY, {});
	for (const [id, values] of Object.entries(stored)) {
		for (const [name, value] of Object.entries(values)) {
			nt.setValue(tunableTopic(id, name), value, "double");
		}
	}
}

// Live samples ----------------------------------------------------------------------

export type Sample = {
	time: number;
	goal: number;
	output: number;
	measured: number;
	volts: number;
	estimate: number | null;
	position: number;
	velocity: number;
	plots: Record<string, number>;
};

function toSample(values: number[]): Sample | undefined {
	if (!Array.isArray(values) || values.length < 8) return undefined;
	const [time, goal, output, measured, volts, estimate, position, velocity] =
		values;
	const names = window.coderunner?.nt.getValue<string[]>(PLOT_NAMES) ?? [];
	const plots: Record<string, number> = {};
	names.forEach((name, i) => {
		const value = values[8 + i];
		if (typeof value === "number") plots[name] = value;
	});
	return {
		time,
		goal,
		output,
		measured,
		volts,
		estimate: Number.isFinite(estimate) ? estimate : null,
		position,
		velocity,
		plots,
	};
}

/** Keep this many seconds of samples in the sandbox. */
const SANDBOX_WINDOW = 6;

/**
 * Every sample of the current run, at the robot's 50 Hz. Re-renders at most
 * once per animation frame.
 */
function useLiveSamples(runId: string, mode: string): Sample[] {
	const buffer = useRef<Sample[]>([]);
	const [samples, setSamples] = useState<Sample[]>([]);
	const frame = useRef<number | null>(null);
	const modeRef = useRef(mode);
	modeRef.current = mode;

	// A new run starts a new buffer.
	useEffect(() => {
		if (runId === "") return;
		buffer.current = [];
		setSamples([]);
	}, [runId]);

	useEffect(() => {
		const flush = () => {
			frame.current = null;
			setSamples(buffer.current.slice());
		};
		const unsubscribe =
			window.coderunner?.nt.subscribe<number[] | string[]>(
				[PLOT_NAMES, SAMPLE],
				(_value, entry) => {
					if (entry.topic !== SAMPLE) return;
					const sample = toSample(entry.value as number[]);
					if (!sample) return;
					const list = buffer.current;
					const last = list[list.length - 1];
					if (last && sample.time <= last.time) {
						if (sample.time === last.time) return;
						// Time went backwards: a new run.
						buffer.current = [];
					}
					buffer.current.push(sample);
					if (modeRef.current === "sandbox") {
						const cutoff = sample.time - SANDBOX_WINDOW;
						while (buffer.current.length > 0 && buffer.current[0].time < cutoff) {
							buffer.current.shift();
						}
					}
					if (frame.current === null) {
						frame.current = window.requestAnimationFrame(flush);
					}
				},
				{ periodic: 0.02 },
			) ?? noop;
		return () => {
			unsubscribe();
			if (frame.current !== null) window.cancelAnimationFrame(frame.current);
		};
	}, []);

	return samples;
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
	/** Id of the running challenge, or "". */
	running: string;
	mode: RunMode | "";
	runId: string;
	error: string;
	stalled: boolean;
	payload: boolean;
	shots: number;
	samples: Sample[];
	results: Record<string, TrialResult>;
	run(id: string, mode: RunMode): void;
	stop(): void;
	setSandboxGoal(goal: number): void;
	sandboxEvent(event: "shot" | "payload" | "bump"): void;
};

export function useLab(): Lab {
	const connected = useNTConnection();
	const [catalogText] = useNTValue<string>(CATALOG);
	const [running] = useNTValue<string>(RUNNING, "");
	const [mode] = useNTValue<string>(MODE, "");
	const [runId] = useNTValue<string>(RUN_ID, "");
	const [error] = useNTValue<string>(ERROR, "");
	const [payload] = useNTValue<boolean>(PAYLOAD, false);
	const [shots] = useNTValue<number>(SHOTS, 0);
	const [, setRequest] = useNTValue<string>(RUN_REQUEST);
	const samples = useLiveSamples(runId, mode);
	const results = useResults();
	const stalled = useRobotStalled(connected && running !== "");

	const catalog = useMemo(
		() => parseJson<{ challenges: CatalogEntry[] }>(catalogText)?.challenges,
		[catalogText],
	);

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
		restoreTunables();
		const last = lastRun.current;
		if (last === null) return;
		// Give the new robot program a moment to set up the lab.
		const timer = window.setTimeout(() => run(last.id, last.mode), 1000);
		return () => window.clearTimeout(timer);
	}, [connected, run]);

	const setSandboxGoal = useCallback((goal: number) => {
		window.coderunner?.nt.setValue(SANDBOX_GOAL, goal, "double");
	}, []);
	const sandboxEvent = useCallback((event: string) => {
		window.coderunner?.nt.setValue(
			SANDBOX_EVENT,
			`${event}#${Date.now()}`,
			"string",
		);
	}, []);

	return {
		connected,
		catalog: connected ? catalog : undefined,
		running: connected ? running : "",
		mode: connected && (mode === "trial" || mode === "sandbox") ? mode : "",
		runId: connected ? runId : "",
		error: connected ? error : "",
		stalled,
		payload,
		shots,
		samples,
		results,
		run,
		stop,
		setSandboxGoal,
		sandboxEvent,
	};
}
