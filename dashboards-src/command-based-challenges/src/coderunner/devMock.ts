import { CHALLENGES } from "../challenges";
import { FIELD } from "../field";
import type { CatalogEntry, TrialResult } from "../lab";
import type { CodeRunnerApi, CodeRunnerContext, NTEntry, NTTopicInfo, Unsubscribe } from "./types";

/**
 * A stand-in `window.coderunner` for `bun run dev` outside CodeRunner.
 *
 * It keeps NetworkTables values in memory and plays a rough copy of
 * CommandLab.java: a robot that drives a loop, intakes, spins up and shoots,
 * with matching command events for the timeline. Trials "start" a second after
 * Run trial and pass every check except one, to show a failed result. Only
 * installed in development, and only when CodeRunner didn't inject the real API.
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
		const previous = values.get(topic)?.value;
		if (previous !== undefined && JSON.stringify(previous) === JSON.stringify(value)) return;
		const entry: NTEntry = { topic, type, value, timestamp: Date.now() * 1000 };
		values.set(topic, entry);
		for (const sub of subs) if (matches(sub, topic)) sub.callback(value, entry);
	}
	const get = <T>(topic: string, fallback: T): T => (values.get(topic)?.value as T) ?? fallback;

	const OUT = "/AdvantageKit/RealOutputs/CommandLab";
	const RUN = "/SmartDashboard/CommandLab/RunRequest";

	const catalog: CatalogEntry[] = CHALLENGES.map((c) => ({
		id: c.id,
		mode: c.mode,
		duration: c.id === "match-play" ? 60 : c.mode === "auto" ? 15 : 6,
		driverControlled: c.id === "match-play",
		checks: [
			{ label: "Does the first thing", bonus: false },
			{ label: "Does the second thing", bonus: false },
			{ label: "Does it with style", bonus: true },
		],
	}));
	put("/CommandLab/Catalog", "string", JSON.stringify({ challenges: catalog }));
	put("/CommandLab/Result", "string", "");
	put(RUN, "string", "");

	let lastRequest = "";
	let running = "";
	let mode = "";
	let phase = "";
	let runId = "";
	let run = 1;
	let time = 0;
	let waitLoops = 0;
	let seq = 0;
	const events: string[] = [];
	const open = new Map<string, string>();
	const event = (type: string, name: string, reqs: string) => {
		seq++;
		events.push(`${seq}\t${run}\t${time.toFixed(3)}\t${type}\t${name}\t${reqs}\t`);
		while (events.length > 80) events.shift();
		if (type === "start") open.set(name, reqs);
		else open.delete(name);
	};
	let pieces: [number, number][] = [
		[2.6, 4.035],
		[2.9, 5.9],
		[2.9, 2.17],
		[6.8, 4.035],
	];
	const shots: number[][] = [];
	let shotId = 0;
	let score = 0;

	window.setInterval(() => {
		const request = get(RUN, "");
		if (request !== lastRequest) {
			lastRequest = request;
			const command = request.slice(0, Math.max(request.lastIndexOf("#"), 0));
			const [id, runMode] = command.split("|");
			for (const name of [...open.keys()]) event("interrupt", name, open.get(name) ?? "");
			running = id ?? "";
			mode = running ? (runMode === "free" ? "free" : "trial") : "";
			phase = running ? (mode === "trial" ? "waiting" : "running") : "";
			runId = request;
			run++;
			time = 0;
			waitLoops = 0;
			score = 0;
			shots.length = 0;
			if (running) event("start", "Drive with sticks", "Drive");
		}
		if (phase === "waiting" && ++waitLoops > 50) phase = "running";

		const t = time;
		// A loop around the field, with a shot every 6 seconds.
		const cycle = t % 6;
		const angle = (t / 6) * Math.PI * 2;
		const x = 2.4 + Math.cos(angle) * 0.9;
		const y = 4.035 + Math.sin(angle) * 1.6;
		const heading = Math.atan2(FIELD.hub.y - y, FIELD.hub.x - x);
		const intaking = cycle > 1 && cycle < 2.4;
		const spinning = cycle > 2.4 && cycle < 4.2;
		const rpm = spinning ? Math.min((cycle - 2.4) / 0.9, 1) * 3000 : cycle > 4.2 ? Math.max(0, 3000 - (cycle - 4.2) * 2500) : 0;
		const piece = cycle < 1.6 ? -1 : cycle < 3.6 ? Math.min((cycle - 1.6) * 3, 0.95) : cycle < 3.75 ? 1.2 : -1;
		if (Math.abs(cycle - 1) < 0.011 && phase === "running") event("start", "Intake", "Indexer,Intake");
		if (Math.abs(cycle - 2.4) < 0.011 && phase === "running") {
			event("finish", "Intake", "Indexer,Intake");
			event("start", "Shoot", "Indexer,Shooter");
		}
		if (Math.abs(cycle - 3.75) < 0.011 && phase === "running") {
			shotId++;
			shots.push([shotId, x, y, FIELD.hub.x, FIELD.hub.y, t, 0.6, 1, 3000]);
			while (shots.length > 6) shots.shift();
		}
		if (Math.abs(cycle - 4.35) < 0.011 && phase === "running") {
			score++;
			event("finish", "Shoot", "Indexer,Shooter");
		}
		const led = piece >= 0.9 ? [0, 200, 0] : rpm > 2850 ? [255, 255, 255] : [0, 80, 255];

		put(`${OUT}/Running`, "string", running);
		put(`${OUT}/Mode`, "string", mode);
		put(`${OUT}/Phase`, "string", phase);
		put(
			`${OUT}/Waiting`,
			"string",
			phase === "waiting" ? "Set the Driver Station to Teleop and click Enable to start the trial." : "",
		);
		put(`${OUT}/Notice`, "string", "");
		put(`${OUT}/RunId`, "string", runId);
		put(`${OUT}/Error`, "string", "");
		put(`${OUT}/Time`, "double", time);
		put(`${OUT}/Duration`, "double", mode === "trial" ? 6 : 0);
		put(`${OUT}/State`, "double[]", [
			time, x, y, heading, 0, 0, 0, intaking ? 1 : 0, intaking ? 1 : 0, intaking ? 1 : 0, piece, rpm,
			spinning ? 3000 : 0, 0, 0, 0, ...led, score, 0, 0, 1, 0, intaking ? 14 : 4, piece >= 0.9 ? 1 : 0, 0, x, y,
			heading, 0, rpm > 2850 ? 1 : 0, run,
		]);
		put(`${OUT}/Pieces`, "double[]", pieces.flat());
		put(`${OUT}/Shots`, "double[]", shots.flat());
		put(`${OUT}/Path`, "double[]", []);
		put(`${OUT}/Controller`, "double[]", [1, intaking ? 16 : 0, -1, 0, 0, 0, spinning ? 1 : 0, 0, 0, 0]);
		put(`${OUT}/Commands`, "string[]", [...open].map(([name, reqs]) => `${name}\t${reqs}`));
		put(
			`${OUT}/Subsystems`,
			"string[]",
			["Drive", "Intake", "Indexer", "Shooter", "Leds"].map((name) => {
				const current = [...open].find(([, reqs]) => reqs.split(",").includes(name))?.[0] ?? "";
				return `${name}\t${current}\t${name === "Drive" ? "Drive with sticks" : ""}`;
			}),
		);
		put(`${OUT}/Events`, "string[]", events.slice());

		if (running && phase !== "waiting") time += 0.02;
		if (mode === "trial" && phase === "running" && time >= 6) {
			const result: TrialResult = {
				id: running,
				runId,
				passed: running !== "spin-up",
				stars: running === "spin-up" ? 0 : 1,
				score,
				checks: [
					{ label: "Does the first thing", bonus: false, pass: true, detail: "did it at 1.20 s" },
					{
						label: "Does the second thing",
						bonus: false,
						pass: running !== "spin-up",
						detail: running === "spin-up" ? "at 3.20 s: the shooter was still asked for 3000 RPM" : "done",
					},
					{ label: "Does it with style", bonus: true, pass: running !== "spin-up", detail: "stylish" },
				],
			};
			put("/CommandLab/Result", "string", JSON.stringify(result));
			phase = "done";
			mode = "free";
		}
		if (pieces.length < 3) pieces = [...pieces, [1.2 + Math.random(), 1 + Math.random()]];
	}, 20);

	const on = <T>(set: Set<T>, listener: T): Unsubscribe => {
		set.add(listener);
		return () => {
			set.delete(listener);
		};
	};
	const inferType = (value: unknown) =>
		typeof value === "boolean" ? "boolean" : typeof value === "number" ? "double" : typeof value === "string" ? "string" : "json";

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
					for (const entry of values.values()) if (matches(sub, entry.topic)) sub.callback(entry.value, entry);
				}
				return () => {
					subs.delete(sub);
				};
			},
			getValue: <T>(topic: string) => values.get(topic)?.value as T | undefined,
			getEntry: <T>(topic: string) => values.get(topic) as NTEntry<T> | undefined,
			setValue(topic, value, type) {
				put(topic, type ?? topics.get(topic)?.type ?? inferType(value), value);
			},
			getTopics: () => [...topics.values()],
			getTopic: (name) => topics.get(name),
			onTopicsChange: (listener) => on(topicListeners, listener),
		},
	};

	window.coderunner = mock;
	console.info("[coderunner] Not running inside CodeRunner: using an in-memory mock of the Command Lab.");
}
