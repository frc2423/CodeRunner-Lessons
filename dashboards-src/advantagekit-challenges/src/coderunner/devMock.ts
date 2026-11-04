import { CHALLENGES, findChallenge } from "../challenges";
import type { CatalogEntry, LogItem, ReplayInfo, Status, TrialResult } from "../lab";
import type { CodeRunnerApi, CodeRunnerContext, NTEntry, NTTopicInfo, Unsubscribe } from "./types";

/**
 * A stand-in `window.coderunner` for `bun run dev` outside CodeRunner.
 *
 * It keeps NetworkTables values in memory and plays a rough copy of AKitLab.java:
 * a flywheel that spins up while A is held, the keys a challenge expects, and a
 * result at the end. The checks are placeholders. "No Peeking" shows a replay that
 * doesn't match, so that the failed view can be styled. Only installed in
 * development, and only when CodeRunner didn't inject the real API.
 */
export function installDevMock(): void {
	if (window.coderunner) return;

	type Sub = { topics: string[]; prefix: boolean; callback: (value: unknown, entry: NTEntry) => void };
	const values = new Map<string, NTEntry>();
	const topics = new Map<string, NTTopicInfo>();
	const subs = new Set<Sub>();
	const topicListeners = new Set<(topics: NTTopicInfo[]) => void>();
	const contextListeners = new Set<(context: CodeRunnerContext) => void>();

	const context: CodeRunnerContext = {
		bridgeVersion: 1,
		theme: window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light",
		dashboard: { id: "dev", title: "Dev dashboard" },
		moduleId: null,
		robot: { running: true, enabled: true, mode: "teleop", eStopped: false, alliance: "blue1" },
	};

	const matches = (sub: Sub, name: string) =>
		sub.prefix ? sub.topics.some((pattern) => name.startsWith(pattern)) : sub.topics.includes(name);

	function put(topic: string, type: string, value: unknown): void {
		if (!topics.has(topic)) {
			topics.set(topic, { name: topic, type, properties: {} });
			const list = [...topics.values()];
			for (const listener of topicListeners) listener(list);
		}
		const entry: NTEntry = { topic, type, value, timestamp: Date.now() * 1000 };
		values.set(topic, entry);
		for (const sub of subs) if (matches(sub, topic)) sub.callback(value, entry);
	}
	const get = <T>(topic: string, fallback: T): T => (values.get(topic)?.value as T) ?? fallback;

	const RUN = "/SmartDashboard/AKitLab/RunRequest";
	const logOnlyIds = ["hindsight", "mystery"];
	const noReplayIds = ["example", "first-output", "every-loop", "poses", "auto-log-output"];

	const catalog: CatalogEntry[] = CHALLENGES.map((c) => ({
		id: c.id,
		duration: logOnlyIds.includes(c.id) ? 0 : 5,
		replays: !noReplayIds.includes(c.id),
		logOnly: logOnlyIds.includes(c.id),
		checks: [
			{ label: "Does the first thing", bonus: false },
			{ label: "Does the second thing", bonus: false },
			...(noReplayIds.includes(c.id) ? [] : [{ label: "The replay matches the real run", bonus: false }]),
			{ label: "Does it with style", bonus: true },
		],
	}));
	put("/AKitLab/Catalog", "string", JSON.stringify({ challenges: catalog }));
	put("/AKitLab/Result", "string", "");
	put(RUN, "string", "");

	let lastRequest = "";
	let running = "";
	let mode: Status["mode"] = "";
	let phase: Status["phase"] = "";
	let runId = "";
	let time = 0;
	let loops = 0;
	let replayTimer = 0;
	let rpm = 0;
	let hasPiece = false;
	let trace: Record<string, number[]> = {};

	const addTrace = (id: string, value: number) => {
		const line = trace[id] ?? [];
		if (line[line.length - 1] !== value) line.push(time, value);
		trace[id] = line;
	};

	/** Roughly what the student's finished code would log. */
	function makeItems(id: string, target: number): LogItem[] {
		const challenge = findChallenge(id);
		const items: LogItem[] = (challenge?.panels.log ?? []).map((expected) => {
			const type = expected.key.includes("HasPiece") || expected.key.endsWith("Ready") || expected.key.endsWith("AtSpeed") ? "boolean" : "double";
			let value: number | boolean = rpm;
			if (type === "boolean") value = rpm > 2900;
			else if (expected.key.includes("Setpoint") || expected.key.includes("Target")) value = target;
			else if (expected.key.includes("Presses")) value = Math.floor(time);
			else if (expected.key.includes("Volts")) value = target / 500;
			return { key: expected.key, kind: expected.kind, type, value };
		});
		if (challenge?.panels.field) {
			const x = 3 + Math.min(time, 3);
			const y = 3 + Math.max(time - 3, 0);
			items.length = 0;
			items.push({ key: "Odometry/Robot", kind: "output", type: "struct:Pose2d", value: { x, y, r: time / 3 } });
			items.push({
				key: "Odometry/Waypoints",
				kind: "output",
				type: "struct:Pose2d[]",
				value: [
					{ x: 3, y: 3, r: 0 },
					{ x: 6, y: 3, r: 0 },
					{ x: 6, y: 5, r: 1.57 },
				],
			});
		}
		if (challenge?.panels.commands) {
			items.push({ key: "CommandsAll/SpinUp", kind: "output", type: "boolean", value: time > 0.5 && time < 2 });
			items.push({ key: "CommandsAll/Feed", kind: "output", type: "boolean", value: time > 1 && time < 1.5 });
			items.push({ key: "CommandsAll/Eject", kind: "output", type: "boolean", value: time > 3 && time < 3.6 });
		}
		return items;
	}

	function makeReplay(id: string): ReplayInfo {
		const speed = trace["truth:rpm"] ?? [0, 0];
		const fails = id === "no-peeking";
		const key = findChallenge(id)?.panels.replayKeys?.[0] ?? "Flywheel/CommandedVolts";
		const series: ReplayInfo["series"] = {
			[key]: { real: speed, replay: fails ? speed.map((v, i) => (i % 2 ? 0 : v)) : speed },
			"Flywheel/CommandedVolts": { real: [0, 0, 1, 6], replay: [0, 0, 1, 6] },
		};
		if (id === "hindsight") series["Analysis/ErrorRPM"] = { replay: speed.map((v, i) => (i % 2 ? 3000 - v : v)) };
		return {
			duration: Math.max(time, 5),
			cycles: 250,
			touches: fails ? 498 : 0,
			firstTouch: fails ? "AutoFeeder.java, line 26" : "",
			crash: "",
			differences: fails ? [{ key, time: 0.22, real: "true", replay: "false", cycles: 120 }] : [],
			series,
			inputs: { "Flywheel/VelocityRadPerSec": speed.map((v, i) => (i % 2 ? v / 9.55 : v)) },
		};
	}

	function finish(id: string): void {
		const fails = id === "no-peeking";
		const replays = catalog.find((c) => c.id === id)?.replays;
		const result: TrialResult = {
			id,
			runId,
			passed: !fails,
			stars: fails ? 0 : 1,
			checks: [
				{ label: "Does the first thing", bonus: false, pass: true, detail: "it did" },
				{ label: "Does the second thing", bonus: false, pass: true, detail: "it did" },
				...(replays
					? [
							{
								label: "The replay matches the real run",
								bonus: false,
								pass: !fails,
								detail: fails ? '1 output differs. First: "AutoFeeder/Feeding" at 0.22 s' : "they matched",
							},
						]
					: []),
				{ label: "Does it with style", bonus: true, pass: !fails, detail: "stylish" },
			],
			trace,
			replay: replays ? makeReplay(id) : undefined,
		};
		put("/AKitLab/Result", "string", JSON.stringify(result));
		phase = "done";
		mode = "free";
	}

	window.setInterval(() => {
		loops++;
		const request = get(RUN, "");
		if (request !== lastRequest) {
			lastRequest = request;
			const command = request.slice(0, Math.max(request.lastIndexOf("#"), 0));
			const [id, runMode] = command.split("|");
			running = id ?? "";
			mode = running ? (runMode === "free" ? "free" : "trial") : "";
			runId = request;
			time = 0;
			rpm = 0;
			trace = {};
			hasPiece = true;
			if (logOnlyIds.includes(running) && mode === "trial") {
				phase = "replaying";
				replayTimer = 40;
				time = 8;
				trace["truth:rpm"] = [0, 0, 1, 2000, 2, 3000, 4, 2600, 5, 3000];
			} else {
				phase = running ? "running" : "";
			}
		}

		const active = phase === "running" || (mode === "free" && phase !== "done");
		const buttons = (get<number[]>("/SmartDashboard/AKitLab/Virtual", [0])[0] ?? 0) | (mode === "trial" && time > 0.5 && time < 3.5 ? 1 : 0);
		if (active) {
			time += 0.02;
			const target = buttons & 1 ? 3000 : 0;
			rpm += (target - rpm) * 0.04;
			if (time > 1.5) hasPiece = false;
			addTrace("truth:rpm", Math.round(rpm));
			addTrace("truth:yawDeg", Math.round(time * 8));
			addTrace("truth:pivotDeg", Math.min(time * 40, 90));
			if (running) {
				const items = makeItems(running, target);
				for (const item of items) {
					const value = typeof item.value === "boolean" ? (item.value ? 1 : 0) : item.value;
					if (typeof value === "number") addTrace(`${item.kind}:${item.key}`, value);
				}
				if (loops % 5 === 0) put("/AKitLab/Tree", "string", JSON.stringify({ time, items }));
			}
			if (phase === "running" && mode === "trial" && time >= 5) {
				if (catalog.find((c) => c.id === running)?.replays) {
					phase = "replaying";
					replayTimer = 80;
				} else {
					finish(running);
				}
			}
		}
		if (phase === "replaying" && --replayTimer <= 0) finish(running);

		const status: Status = {
			running,
			mode,
			phase,
			notice: "",
			runId,
			error: "",
			loops,
			time,
			duration: running ? 5 : 0,
			scripted: mode === "trial" && phase === "running",
			buttons,
			hasPiece,
			shots: 0,
			rpm,
			yawDeg: time * 8,
			pivotDeg: Math.min(time * 40, 90),
		};
		put("/AKitLab/Status", "string", JSON.stringify(status));
	}, 20);

	const api: CodeRunnerApi = {
		version: 1,
		embedded: false,
		ready: Promise.resolve(context),
		getContext: () => context,
		onContextChange(listener) {
			contextListeners.add(listener);
			return () => contextListeners.delete(listener);
		},
		nt: {
			subscribe<T>(topic: string | string[], callback: (value: T, entry: NTEntry<T>) => void, options?: { prefix?: boolean; immediate?: boolean }): Unsubscribe {
				const sub: Sub = {
					topics: Array.isArray(topic) ? topic : [topic],
					prefix: options?.prefix ?? false,
					callback: callback as Sub["callback"],
				};
				subs.add(sub);
				if (options?.immediate !== false) {
					for (const [name, entry] of values) if (matches(sub, name)) sub.callback(entry.value, entry);
				}
				return () => subs.delete(sub);
			},
			getValue: <T>(topic: string) => values.get(topic)?.value as T | undefined,
			getEntry: <T>(topic: string) => values.get(topic) as NTEntry<T> | undefined,
			setValue: (topic: string, value: unknown, type?: string) => put(topic, type ?? "string", value),
			isConnected: () => true,
			onConnectionChange: () => () => {},
			getTopics: () => [...topics.values()],
			getTopic: (name: string) => topics.get(name),
			onTopicsChange(listener) {
				topicListeners.add(listener);
				return () => topicListeners.delete(listener);
			},
		},
	};
	window.coderunner = api;
}
