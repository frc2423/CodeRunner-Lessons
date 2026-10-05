import { useCallback, useSyncExternalStore } from "react";
import type {
	CodeRunnerApi,
	CodeRunnerContext,
	CodeRunnerRobotState,
	NTTopicInfo,
} from "./types";

/**
 * React hooks over `window.coderunner`. Each one is a thin
 * `useSyncExternalStore` wrapper, so components re-render only when the value
 * they read changes. Copy this file into any React dashboard.
 */

function api(): CodeRunnerApi | undefined {
	return window.coderunner;
}

const noop = () => {};
const EMPTY_TOPICS: NTTopicInfo[] = [];

/**
 * Read and write one NetworkTables topic.
 *
 * ```tsx
 * const [speed, setSpeed] = useNTValue<number>("/SmartDashboard/Speed", 0);
 * ```
 *
 * The setter takes an optional NT4 type for values whose type cannot be
 * inferred (e.g. `setCount(3, "int")`).
 */
export function useNTValue<T>(
	topic: string,
	defaultValue: T,
): [T, (value: T, type?: string) => void];
export function useNTValue<T>(
	topic: string,
): [T | undefined, (value: T, type?: string) => void];
export function useNTValue<T>(
	topic: string,
	defaultValue?: T,
): [T | undefined, (value: T, type?: string) => void] {
	const subscribe = useCallback(
		(onChange: () => void) => api()?.nt.subscribe(topic, onChange) ?? noop,
		[topic],
	);
	const value = useSyncExternalStore(subscribe, () =>
		api()?.nt.getValue<T>(topic),
	);
	const setValue = useCallback(
		(next: T, type?: string) => api()?.nt.setValue(topic, next, type),
		[topic],
	);
	return [value ?? defaultValue, setValue];
}

/** Whether CodeRunner has a live NetworkTables connection to the robot program. */
export function useNTConnection(): boolean {
	return useSyncExternalStore(
		(onChange) => api()?.nt.onConnectionChange(onChange) ?? noop,
		() => api()?.nt.isConnected() ?? false,
	);
}

/** Every announced topic, e.g. for a topic browser. */
export function useNTTopics(): NTTopicInfo[] {
	const subscribe = useCallback(
		(onChange: () => void) => api()?.nt.onTopicsChange(onChange) ?? noop,
		[],
	);
	// getTopics() builds a new array each call; cache it per notification so
	// useSyncExternalStore sees a stable snapshot.
	const snapshot = useCallback(() => topicsSnapshot(), []);
	return useSyncExternalStore(subscribe, snapshot);
}

let cachedTopics: NTTopicInfo[] = EMPTY_TOPICS;
let cachedTopicKey = "";
function topicsSnapshot(): NTTopicInfo[] {
	const topics = api()?.nt.getTopics() ?? EMPTY_TOPICS;
	const key = topics.map((t) => `${t.name}\u0000${t.type}`).join("\u0001");
	if (key !== cachedTopicKey) {
		cachedTopicKey = key;
		cachedTopics = topics;
	}
	return cachedTopics;
}

/** Theme, lesson and robot state from CodeRunner; null until it arrives. */
export function useCodeRunnerContext(): CodeRunnerContext | null {
	return useSyncExternalStore(
		(onChange) => api()?.onContextChange(onChange) ?? noop,
		() => api()?.getContext() ?? null,
	);
}

/** Driver Station state (enabled, mode, alliance…), or null. */
export function useRobotState(): CodeRunnerRobotState | null {
	return useCodeRunnerContext()?.robot ?? null;
}
