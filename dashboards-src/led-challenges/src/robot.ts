import { useCallback, useEffect, useRef, useState, useSyncExternalStore } from "react";
import { useNTConnection, useNTValue } from "./coderunner/hooks";

/**
 * The NetworkTables topics shared with the lesson's `LedSubsystem.java`.
 */

/** Written by the dashboard: `"<challenge id>#<nonce>"`, or `"#<nonce>"` to stop. */
export const RUN_REQUEST = "/SmartDashboard/LEDs/RunRequest";

const OUTPUTS = "/AdvantageKit/RealOutputs/LEDs";
/** Every LED's color, packed as 0xRRGGBB. */
export const COLORS = `${OUTPUTS}/Colors`;
/** Id of the running challenge, or "" when none is running. */
export const RUNNING = `${OUTPUTS}/Running`;
/** Seconds since the running challenge started. Changes every robot loop. */
export const SECONDS = `${OUTPUTS}/Seconds`;
/** Why the last challenge stopped, or "". */
export const ERROR = `${OUTPUTS}/Error`;
/** Every challenge id the robot program knows. */
export const CHALLENGE_IDS = `${OUTPUTS}/Challenges`;

const noop = () => {};

/** The strip's colors, at the robot's full 50 Hz rate. */
export function useLedColors(): number[] | undefined {
	const subscribe = useCallback(
		(onChange: () => void) =>
			window.coderunner?.nt.subscribe(COLORS, onChange, { periodic: 0.02 }) ??
			noop,
		[],
	);
	return useSyncExternalStore(subscribe, () =>
		window.coderunner?.nt.getValue<number[]>(COLORS),
	);
}

/**
 * True when a challenge is running but the robot has stopped sending updates,
 * which almost always means student code is stuck in a loop that never ends.
 */
export function useRobotStalled(running: boolean, connected: boolean): boolean {
	const lastUpdate = useRef(Date.now());
	const [stalled, setStalled] = useState(false);

	useEffect(
		() =>
			window.coderunner?.nt.subscribe(
				SECONDS,
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
		if (!running || !connected) return;
		const timer = window.setInterval(() => {
			setStalled(Date.now() - lastUpdate.current > 2000);
		}, 500);
		return () => window.clearInterval(timer);
	}, [running, connected]);

	return stalled;
}

export type RobotLeds = {
	connected: boolean;
	colors: number[] | undefined;
	/** Id of the running challenge, or "". */
	running: string;
	error: string;
	/** Challenge ids the robot program knows, once it has published them. */
	knownIds: string[] | undefined;
	stalled: boolean;
	run(id: string): void;
	stop(): void;
};

/** Everything the dashboard needs from the robot's LED subsystem. */
export function useRobotLeds(): RobotLeds {
	const connected = useNTConnection();
	const colors = useLedColors();
	const [running] = useNTValue<string>(RUNNING, "");
	const [error] = useNTValue<string>(ERROR, "");
	const [knownIds] = useNTValue<string[]>(CHALLENGE_IDS);
	const [, setRequest] = useNTValue<string>(RUN_REQUEST);
	const stalled = useRobotStalled(connected && running !== "", connected);

	// The last challenge the student ran, so it can run again by itself after
	// they edit their code and restart the robot program.
	const lastRun = useRef<string | null>(null);

	const run = useCallback(
		(id: string) => {
			lastRun.current = id;
			setRequest(`${id}#${Date.now()}`, "string");
		},
		[setRequest],
	);
	const stop = useCallback(() => {
		lastRun.current = null;
		setRequest(`#${Date.now()}`, "string");
	}, [setRequest]);

	const wasConnected = useRef(connected);
	useEffect(() => {
		const reconnected = connected && !wasConnected.current;
		wasConnected.current = connected;
		const id = lastRun.current;
		if (!reconnected || id === null) return;
		// Give the new robot program a moment to set up its LED subsystem.
		const timer = window.setTimeout(() => run(id), 1000);
		return () => window.clearTimeout(timer);
	}, [connected, run]);

	return {
		connected,
		colors: connected ? colors : undefined,
		running: connected ? running : "",
		error: connected ? error : "",
		knownIds,
		stalled,
		run,
		stop,
	};
}
