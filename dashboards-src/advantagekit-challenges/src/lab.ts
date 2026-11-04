import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNTConnection, useNTValue } from "./coderunner/hooks";

/**
 * The NetworkTables protocol shared with the lesson's `AKitLab.java`, and the
 * hooks the dashboard reads the lab through.
 *
 * The lab talks over plain topics under `/AKitLab/` rather than through
 * AdvantageKit, so the student's log only holds what the student put there.
 */

// Dashboard → robot (SmartDashboard table) --------------------------------------

/** `"<challenge id>|<trial|free>#<nonce>"`, or `"#<nonce>"` to stop. */
export const RUN_REQUEST = "/SmartDashboard/AKitLab/RunRequest";
/** `[button bits, six axes]`, axes in `XboxController.Axis` order. */
export const VIRTUAL = "/SmartDashboard/AKitLab/Virtual";
/** `"piece#<nonce>"` puts a game piece in the feeder, in free play only. */
export const FIELD_EVENT = "/SmartDashboard/AKitLab/FieldEvent";

export const TUNING_NEW = "/Tuning/TargetRPM";
export const TUNING_OLD = "/SmartDashboard/TargetRPM";
export const PRESET = "/SmartDashboard/ShotPreset/selected";

// Robot → dashboard -------------------------------------------------------------

export const CATALOG = "/AKitLab/Catalog";
export const RESULT = "/AKitLab/Result";
export const STATUS = "/AKitLab/Status";
export const TREE = "/AKitLab/Tree";

export type CatalogEntry = {
	id: string;
	duration: number;
	replays: boolean;
	/** The challenge replays a shipped log, with no trial to run first. */
	logOnly: boolean;
	checks: { label: string; bonus: boolean }[];
};

export type CheckResult = {
	label: string;
	bonus: boolean;
	pass: boolean;
	detail: string;
};

export type Difference = {
	key: string;
	time: number;
	real: string;
	replay: string;
	cycles: number;
};

/** A line as flat pairs: `[time, value, time, value, …]`, one pair per change. */
export type Points = number[];

export type ReplaySeries = {
	real?: Points;
	realLabels?: string[];
	replay?: Points;
	replayLabels?: string[];
};

export type ReplayInfo = {
	duration: number;
	cycles: number;
	touches: number;
	firstTouch: string;
	crash: string;
	differences: Difference[];
	series: Record<string, ReplaySeries>;
	inputs: Record<string, Points>;
};

export type TrialResult = {
	id: string;
	runId: string;
	passed: boolean;
	stars: number;
	checks: CheckResult[];
	/** Everything the trial logged: `output:<key>`, `input:<key>`, `dashboard:<key>`, `truth:<name>`. */
	trace: Record<string, Points>;
	replay?: ReplayInfo;
	replayError?: string;
};

export type Status = {
	running: string;
	mode: "" | "trial" | "free";
	phase: "" | "waiting" | "running" | "replaying" | "done";
	notice: string;
	runId: string;
	error: string;
	loops: number;
	time: number;
	duration: number;
	scripted: boolean;
	buttons: number;
	hasPiece: boolean;
	shots: number;
	rpm: number;
	yawDeg: number;
	pivotDeg: number;
};

export type LogValue = number | boolean | string | Pose | Pose[] | (number | boolean | string)[];
export type Pose = { x: number; y: number; r: number };

export type LogItem = {
	key: string;
	kind: "input" | "output" | "dashboard";
	type: string;
	unit?: string;
	value: LogValue;
};

export type Tree = { time: number; items: LogItem[] };

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

/**
 * True when something is loaded but the robot has stopped sending updates,
 * which almost always means student code is stuck in a loop that never ends.
 */
function useRobotStalled(active: boolean, loops: number): boolean {
	const lastUpdate = useRef(Date.now());
	const [stalled, setStalled] = useState(false);

	useEffect(() => {
		lastUpdate.current = Date.now();
		setStalled(false);
	}, [loops]);

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

/** Keep this many seconds of history in free play. */
const FREE_PLAY_WINDOW = 15;

/**
 * What's been logged during this run, for the live plots, in the same shape as
 * a result's trace.
 */
function useHistory(status: Status | undefined, tree: Tree | undefined): Record<string, Points> {
	const [history, setHistory] = useState<Record<string, Points>>({});
	const runId = status?.runId ?? "";
	const buffer = useRef<Record<string, Points>>({});

	// A new run starts a new history. So does the robot clock going back.
	const lastTime = useRef(0);
	const reset = () => {
		buffer.current = {};
		lastTime.current = 0;
		setHistory({});
	};
	useEffect(reset, [runId]);

	const add = useCallback((id: string, time: number, value: number) => {
		const line = buffer.current[id] ?? [];
		const previous = line[line.length - 1];
		if (previous === value) return;
		line.push(time, value);
		buffer.current[id] = line;
	}, []);

	const phase = status?.phase;
	const mode = status?.mode;
	const time = status?.time ?? 0;
	const rpm = status?.rpm ?? 0;
	const yawDeg = status?.yawDeg ?? 0;
	const pivotDeg = status?.pivotDeg ?? 0;

	useEffect(() => {
		const collecting = phase === "running";
		if (!collecting) return;
		if (time < lastTime.current) buffer.current = {};
		lastTime.current = time;
		add("truth:rpm", time, rpm);
		add("truth:yawDeg", time, yawDeg);
		add("truth:pivotDeg", time, pivotDeg);
	}, [phase, mode, time, rpm, yawDeg, pivotDeg, add]);

	useEffect(() => {
		if (!tree) return;
		if (phase !== "running") return;
		for (const item of tree.items) {
			const number =
				typeof item.value === "number"
					? item.value
					: typeof item.value === "boolean"
						? item.value
							? 1
							: 0
						: undefined;
			if (number !== undefined) add(`${item.kind}:${item.key}`, tree.time, number);
		}
		if (mode === "free") {
			const cutoff = tree.time - FREE_PLAY_WINDOW;
			for (const line of Object.values(buffer.current)) {
				let drop = 0;
				while (drop + 2 < line.length && line[drop + 2] < cutoff) drop += 2;
				if (drop > 0) line.splice(0, drop);
			}
		}
		setHistory({ ...buffer.current });
	}, [tree, phase, mode, add]);

	return history;
}

// Everything together -------------------------------------------------------------

export type Lab = {
	connected: boolean;
	catalog: CatalogEntry[] | undefined;
	status: Status | undefined;
	tree: Tree | undefined;
	history: Record<string, Points>;
	results: Record<string, TrialResult>;
	stalled: boolean;
	run(id: string, mode: "trial" | "free"): void;
	stop(): void;
	setVirtual(buttons: number, axes: number[]): void;
	loadPiece(): void;
	setTuning(rpm: number): void;
	setPreset(name: string): void;
};

export function useLab(): Lab {
	const connected = useNTConnection();
	const [catalogText] = useNTValue<string>(CATALOG);
	const [statusText] = useNTValue<string>(STATUS);
	const [treeText] = useNTValue<string>(TREE);
	const [, setRequest] = useNTValue<string>(RUN_REQUEST);

	const catalog = useMemo(
		() => parseJson<{ challenges: CatalogEntry[] }>(catalogText)?.challenges,
		[catalogText],
	);
	const status = useMemo(() => parseJson<Status>(statusText), [statusText]);
	const tree = useMemo(() => parseJson<Tree>(treeText), [treeText]);
	const results = useResults();
	const history = useHistory(connected ? status : undefined, connected ? tree : undefined);
	const stalled = useRobotStalled(connected && (status?.running ?? "") !== "", status?.loops ?? 0);

	// The last thing the student ran, so it runs again by itself after they
	// edit their code and restart the robot program.
	const lastRun = useRef<{ id: string; mode: "trial" | "free" } | null>(null);

	const run = useCallback(
		(id: string, mode: "trial" | "free") => {
			lastRun.current = { id, mode };
			setRequest(`${id}|${mode}#${Date.now()}`, "string");
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

	const setVirtual = useCallback((buttons: number, axes: number[]) => {
		window.coderunner?.nt.setValue(VIRTUAL, [buttons, ...axes], "double[]");
	}, []);
	const loadPiece = useCallback(() => {
		window.coderunner?.nt.setValue(FIELD_EVENT, `piece#${Date.now()}`, "string");
	}, []);
	const setTuning = useCallback((rpm: number) => {
		window.coderunner?.nt.setValue(TUNING_NEW, rpm, "double");
		window.coderunner?.nt.setValue(TUNING_OLD, rpm, "double");
	}, []);
	const setPreset = useCallback((name: string) => {
		window.coderunner?.nt.setValue(PRESET, name, "string");
	}, []);

	return {
		connected,
		catalog: connected ? catalog : undefined,
		status: connected ? status : undefined,
		tree: connected ? tree : undefined,
		history,
		results,
		stalled,
		run,
		stop,
		setVirtual,
		loadPiece,
		setTuning,
		setPreset,
	};
}
