import { CHALLENGES } from "../challenges";
import { DEFAULT_LED_COUNT, type GoalProgram, LedStrip } from "../leds";
import type {
	CodeRunnerApi,
	CodeRunnerContext,
	NTEntry,
	NTTopicInfo,
	Unsubscribe,
} from "./types";

/**
 * A stand-in `window.coderunner` for `bun run dev` outside CodeRunner.
 *
 * It keeps NetworkTables values in memory and stands in for the lesson's
 * LedSubsystem.java, running each challenge's goal animation as if the student
 * had solved it (except "stripes", which throws, to show the error banner), so
 * the UI can be built and styled without a robot. Writes are stored and echoed back like the real
 * thing. Only installed in development, and only when CodeRunner did not
 * already inject the real API.
 */
export function installDevMock(): void {
	if (window.coderunner) return;

	type Sub = {
		topics: string[];
		prefix: boolean;
		callback: (value: unknown, entry: NTEntry) => void;
	};

	const values = new Map<string, NTEntry>();
	const topics = new Map<string, NTTopicInfo>();
	const subs = new Set<Sub>();
	const topicListeners = new Set<(topics: NTTopicInfo[]) => void>();
	const contextListeners = new Set<(context: CodeRunnerContext) => void>();

	const context: CodeRunnerContext = {
		bridgeVersion: 1,
		theme: window.matchMedia("(prefers-color-scheme: dark)").matches
			? "dark"
			: "light",
		dashboard: { id: "dev", title: "Dev dashboard" },
		moduleId: null,
		robot: {
			running: true,
			enabled: true,
			mode: "teleop",
			eStopped: false,
			alliance: "red1",
		},
	};

	const matches = (sub: Sub, name: string) =>
		sub.prefix
			? sub.topics.some((pattern) => name.startsWith(pattern))
			: sub.topics.includes(name);

	function put(topic: string, type: string, value: unknown): void {
		if (!topics.has(topic)) {
			topics.set(topic, { name: topic, type, properties: {} });
			const list = [...topics.values()];
			for (const listener of topicListeners) listener(list);
		}
		const previous = values.get(topic)?.value;
		if (
			previous !== undefined &&
			JSON.stringify(previous) === JSON.stringify(value)
		) {
			return;
		}
		const entry: NTEntry = { topic, type, value, timestamp: Date.now() * 1000 };
		values.set(topic, entry);
		for (const sub of subs) if (matches(sub, topic)) sub.callback(value, entry);
	}

	function inferType(value: unknown): string {
		if (typeof value === "boolean") return "boolean";
		if (typeof value === "number") return "double";
		if (typeof value === "string") return "string";
		if (Array.isArray(value)) {
			const first = value[0];
			return typeof first === "string"
				? "string[]"
				: typeof first === "boolean"
					? "boolean[]"
					: "double[]";
		}
		return "json";
	}

	// A stand-in for LedSubsystem.java.
	const strip = new LedStrip(DEFAULT_LED_COUNT);
	const outputs = "/AdvantageKit/RealOutputs/LEDs";
	put("/SmartDashboard/LEDs/RunRequest", "string", "");
	let lastRequest = "";
	let running = "";
	let program: GoalProgram | null = null;
	let startTime = 0;
	let seconds = 0;
	let error = "";
	window.setInterval(() => {
		const request = values.get("/SmartDashboard/LEDs/RunRequest")?.value;
		if (typeof request === "string" && request !== lastRequest) {
			lastRequest = request;
			const id = request.slice(0, Math.max(request.lastIndexOf("#"), 0));
			strip.clear();
			error = "";
			seconds = 0;
			running = id;
			program = CHALLENGES.find((c) => c.id === id)?.goal?.() ?? null;
			startTime = performance.now();
			if (id === "stripes") {
				error =
					"IndexOutOfBoundsException: There is no LED 30. This strip has LEDs 0 to 29 (counting starts at 0). (Challenge05Stripes.java, line 14)";
				running = "";
				program = null;
			} else if (id !== "" && !CHALLENGES.some((c) => c.id === id)) {
				error = `The robot program has no challenge called "${id}".`;
				running = "";
			}
			program?.start?.(strip);
		}
		if (running !== "") {
			seconds = (performance.now() - startTime) / 1000;
			const robot = context.robot;
			program?.update(strip, seconds, {
				enabled: robot?.enabled ?? false,
				autonomous: robot?.mode === "auto",
				redAlliance: robot?.alliance.startsWith("red") ?? false,
			});
		}
		put(`${outputs}/Colors`, "int[]", [...strip.colors]);
		put(`${outputs}/Running`, "string", running);
		put(`${outputs}/Seconds`, "double", seconds);
		put(`${outputs}/Error`, "string", error);
		put(`${outputs}/Challenges`, "string[]", CHALLENGES.map((c) => c.id));
	}, 20);

	const on = <T>(set: Set<T>, listener: T): Unsubscribe => {
		set.add(listener);
		return () => {
			set.delete(listener);
		};
	};

	const mock: CodeRunnerApi = {
		version: 1,
		embedded: false,
		ready: Promise.resolve(context),
		getContext: () => context,
		onContextChange: (listener) => on(contextListeners, listener),
		nt: {
			isConnected: () => true,
			onConnectionChange: () => () => {},
			subscribe(topic, callback, options = {}) {
				const sub: Sub = {
					topics: Array.isArray(topic) ? topic : [topic],
					prefix: options.prefix === true,
					callback: callback as Sub["callback"],
				};
				subs.add(sub);
				if (options.immediate !== false) {
					for (const entry of values.values()) {
						if (matches(sub, entry.topic)) sub.callback(entry.value, entry);
					}
				}
				return () => {
					subs.delete(sub);
				};
			},
			getValue: <T>(topic: string) => values.get(topic)?.value as T | undefined,
			getEntry: <T>(topic: string) =>
				values.get(topic) as NTEntry<T> | undefined,
			setValue(topic, value, type) {
				put(topic, type ?? topics.get(topic)?.type ?? inferType(value), value);
			},
			getTopics: () => [...topics.values()],
			getTopic: (name) => topics.get(name),
			onTopicsChange: (listener) => on(topicListeners, listener),
		},
	};

	window.coderunner = mock;
	console.info(
		"[coderunner] Not running inside CodeRunner: using an in-memory mock with sample data.",
	);
}
