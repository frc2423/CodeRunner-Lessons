/**
 * Types for `window.coderunner`, the API CodeRunner injects into every
 * dashboard page. Plain JavaScript dashboards get the same object; these
 * declarations are only here to make TypeScript aware of it.
 */

export type Unsubscribe = () => void;

/** A NetworkTables topic the robot program (or another client) announced. */
export type NTTopicInfo = {
	name: string;
	/** NT4 type string: "double", "string[]", "json", "struct:Pose2d", … */
	type: string;
	properties: Record<string, unknown>;
};

/** The latest value of one topic. */
export type NTEntry<T = unknown> = {
	topic: string;
	type: string;
	/**
	 * Numbers, strings, booleans and their arrays arrive as-is; `json` topics
	 * are parsed; `struct:` topics are decoded into plain objects (for example
	 * a Pose2d is `{ translation: { x, y }, rotation: { value } }`); anything
	 * else is a `Uint8Array`.
	 */
	value: T;
	/** Robot time in microseconds. */
	timestamp: number;
};

export type NTSubscribeOptions = {
	/** Match every topic that starts with the given string(s). */
	prefix?: boolean;
	/** Seconds between updates from the robot. Default 0.05 (20 Hz). */
	periodic?: number;
	/** Call back with the cached value straight away. Default true. */
	immediate?: boolean;
};

export type CodeRunnerRobotState = {
	/** A robot program is built and running in the simulator. */
	running: boolean;
	enabled: boolean;
	mode: "auto" | "teleop" | "test";
	eStopped: boolean;
	alliance: "red1" | "red2" | "red3" | "blue1" | "blue2" | "blue3";
};

export type CodeRunnerContext = {
	bridgeVersion: number;
	/** CodeRunner's current theme, so a dashboard can match it. */
	theme: "light" | "dark";
	dashboard: { id: string; title: string };
	/** The loaded lesson module's id, or null. */
	moduleId: string | null;
	/** Driver Station state; null until the simulator reports it. */
	robot: CodeRunnerRobotState | null;
};

export interface CodeRunnerNetworkTables {
	isConnected(): boolean;
	onConnectionChange(listener: (connected: boolean) => void): Unsubscribe;
	subscribe<T = unknown>(
		topic: string | string[],
		callback: (value: T, entry: NTEntry<T>) => void,
		options?: NTSubscribeOptions,
	): Unsubscribe;
	getValue<T = unknown>(topic: string): T | undefined;
	getEntry<T = unknown>(topic: string): NTEntry<T> | undefined;
	/**
	 * Publish a value. Pass an NT4 `type` for anything that cannot be inferred
	 * (e.g. "int", "float[]", "struct:Pose2d"); otherwise the topic's announced
	 * type is used, or one inferred from the value.
	 */
	setValue(topic: string, value: unknown, type?: string): void;
	getTopics(): NTTopicInfo[];
	getTopic(name: string): NTTopicInfo | undefined;
	onTopicsChange(listener: (topics: NTTopicInfo[]) => void): Unsubscribe;
}

export interface CodeRunnerApi {
	readonly version: number;
	/** True inside a CodeRunner dashboard tab; false for the dev mock. */
	readonly embedded: boolean;
	/** Resolves with the first context CodeRunner sends. */
	readonly ready: Promise<CodeRunnerContext>;
	getContext(): CodeRunnerContext | null;
	onContextChange(listener: (context: CodeRunnerContext) => void): Unsubscribe;
	readonly nt: CodeRunnerNetworkTables;
}

declare global {
	interface Window {
		coderunner?: CodeRunnerApi;
	}
}
