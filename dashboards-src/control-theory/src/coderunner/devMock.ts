import { CHALLENGES } from "../challenges";
import type { CatalogEntry, MechanismKind, TrialResult } from "../lab";
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
 * ControlLab.java: a rough copy of its mechanisms, run by simple solved
 * controllers, so the UI can be built and styled without a robot. Trials are
 * "graded" with every check passing, except Proportional Control, which
 * fails one to show a failed result. Only installed in development, and only
 * when CodeRunner did not already inject the real API.
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
		theme: window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light",
		dashboard: { id: "dev", title: "Dev dashboard" },
		moduleId: null,
		robot: { running: true, enabled: false, mode: "teleop", eStopped: false, alliance: "red1" },
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

	// Mechanisms: volts = kS·sign(v) + kG·gravity + kV·v + kA·a -----------------------

	type Plant = { kS: number; kV: number; kA: number; kG: number; min: number; max: number; rate: number; shot: number };
	const PLANTS: Record<MechanismKind, Plant> = {
		flywheel: { kS: 0.25, kV: 0.002, kA: 0.0006, kG: 0, min: -1e9, max: 1e9, rate: 1 / 60, shot: 10 },
		steering: { kS: 0.15, kV: 0.008, kA: 0.0008, kG: 0, min: -1e9, max: 1e9, rate: 1, shot: 0 },
		elevator: { kS: 0.15, kV: 6, kA: 0.6, kG: 0.45, min: 0, max: 1.6, rate: 1, shot: 0 },
		arm: { kS: 0.1, kV: 1.8, kA: 0.12, kG: 0.9, min: -Math.PI / 4, max: (3 * Math.PI) / 4, rate: 1, shot: 0 },
	};
	const wrap = (d: number) => ((((d + 180) % 360) + 360) % 360) - 180;

	type Script = { duration: number; start?: number; goals: [number, number][]; shots?: number[]; payload?: number; noise?: number };
	const DEG = Math.PI / 180;
	const SCRIPTS: Record<string, Script> = {
		"open-loop": { duration: 3, goals: [[0, 3000]] },
		"bang-bang": { duration: 4.5, goals: [[0, 3000], [2.5, 4500]] },
		proportional: { duration: 3, goals: [[0, 3000]] },
		feedforward: { duration: 5.5, goals: [[0, 3000], [3.8, 4500]], shots: [1.6, 2.3, 3.0] },
		"pd-steering": { duration: 4.5, goals: [[0, 90], [1.5, -45], [3, 30]] },
		"shortest-path": { duration: 4.5, start: 150, goals: [[0, -150], [1.5, 100], [3, -100]] },
		"fight-gravity": { duration: 6, goals: [[0, 0.5], [2, 1.2], [4, 0.3]] },
		"mystery-payload": { duration: 7.5, goals: [[0, 1], [5, 0.4]], payload: 2 },
		"arm-feedforward": { duration: 6, start: -45 * DEG, goals: [[0, 0], [2, 90 * DEG], [4, 30 * DEG]] },
		"motion-profile": { duration: 5, goals: [[0, 1.4], [2.5, 0.2]] },
		sysid: { duration: 8, goals: [[0, 0]], noise: 10 },
		lqr: { duration: 5, goals: [[0, 2000], [1.5, 4000]], shots: [3.2, 3.9] },
		"noisy-sensor": { duration: 5, goals: [[0, 3000]], shots: [2.5, 3.5], noise: 120 },
		"kalman-filter": { duration: 5, goals: [[0, 2000], [1.5, 4000], [3, 2500]], noise: 120 },
		"state-space-elevator": { duration: 7.5, goals: [[0, 1.2], [2.5, 0.4], [5, 1.5]] },
	};

	/** Solved controllers, roughly. */
	function control(id: string, kind: MechanismKind, goal: number, pos: number, vel: number, t: number, state: { last: number; filtered: number }): number {
		switch (kind) {
			case "flywheel": {
				if (id === "sysid") return t < 4 ? 1.5 * t : t < 6 ? 10 : 2;
				if (id === "open-loop") return 6;
				if (id === "bang-bang") return vel < goal ? 12 : 0;
				state.filtered += (vel - state.filtered) * 0.2;
				const speed = SCRIPTS[id]?.noise ? state.filtered : vel;
				return 0.25 + 0.002 * goal + 0.02 * (goal - speed);
			}
			case "steering": {
				const error = wrap(goal - pos);
				const d = (error - state.last) / 0.02;
				state.last = error;
				return 0.2 * error + 0.012 * d;
			}
			case "elevator":
				return 40 * (goal - pos) - 3 * vel + 0.45;
			case "arm":
				return 15 * (goal - pos) - 1 * vel + 0.9 * Math.cos(pos);
		}
	}

	// A stand-in for ControlLab.java ----------------------------------------------------

	const RUN = "/SmartDashboard/ControlLab/RunRequest";
	const GOAL = "/SmartDashboard/ControlLab/Sandbox/Goal";
	const EVENT = "/SmartDashboard/ControlLab/Sandbox/Event";
	const OUT = "/AdvantageKit/RealOutputs/ControlLab";

	const catalog: CatalogEntry[] = CHALLENGES.map((c) => ({
		id: c.id,
		mechanism: c.mechanism,
		units: "",
		duration: SCRIPTS[c.id]?.duration ?? 5,
		problem: "",
		checks: [
			{ label: "Reaches every goal quickly", bonus: false },
			{ label: "Overshoots by less than a little", bonus: false },
			{ label: "Does it even faster", bonus: true },
		],
		tunables: Object.entries(c.sliders ?? {}).map(([name, s]) => ({ name, default: s.log ? s.min * 10 : 0 })),
	}));
	put("/ControlLab/Catalog", "string", JSON.stringify({ challenges: catalog }));
	put("/ControlLab/Result", "string", "");
	put(RUN, "string", "");
	put(GOAL, "double", 0);
	put(EVENT, "string", "");
	for (const c of catalog) for (const t of c.tunables) put(`/SmartDashboard/ControlLab/Tune/${c.id}/${t.name}`, "double", t.default);

	let lastRequest = "";
	let lastEvent = "";
	let running = "";
	let mode = "";
	let runId = "";
	let kind: MechanismKind = "flywheel";
	let script: Script = SCRIPTS["open-loop"];
	let loop = 0;
	let pos = 0;
	let vel = 0;
	let volts = 0;
	let shotLeft = 0;
	let shots = 0;
	let payload = false;
	let state = { last: 0, filtered: 0 };
	const trace = { time: [] as number[], goal: [] as number[], output: [] as number[], measured: [] as number[], volts: [] as number[], estimate: [] as (number | null)[] };

	const noise = () => (Math.random() + Math.random() + Math.random() - 1.5) * 2 * (script.noise ?? 0);

	window.setInterval(() => {
		const request = get(RUN, "");
		if (request !== lastRequest) {
			lastRequest = request;
			const command = request.slice(0, Math.max(request.lastIndexOf("#"), 0));
			const [id, runMode] = command.split("|");
			running = id && SCRIPTS[id] ? id : "";
			mode = running ? (runMode === "sandbox" ? "sandbox" : "trial") : "";
			runId = request;
			if (running) {
				kind = CHALLENGES.find((c) => c.id === id)?.mechanism ?? "flywheel";
				script = SCRIPTS[id];
				loop = 0;
				pos = script.start ?? (kind === "arm" ? -45 * DEG : 0);
				vel = 0;
				shots = 0;
				payload = false;
				state = { last: wrap(script.goals[0][1] - pos), filtered: 0 };
				for (const list of Object.values(trace)) list.length = 0;
				if (mode === "sandbox") put(GOAL, "double", script.goals[0][1]);
			}
		}
		if (running === "") {
			put(`${OUT}/Running`, "string", "");
			put(`${OUT}/Mode`, "string", "");
			return;
		}

		const t = loop * 0.02;
		let goal = script.goals[0][1];
		if (mode === "sandbox") {
			goal = get(GOAL, goal);
			const event = get(EVENT, "");
			if (event !== lastEvent) {
				lastEvent = event;
				if (event.startsWith("shot")) {
					shotLeft = 0.08;
					shots++;
				}
				if (event.startsWith("payload")) payload = !payload;
				if (event.startsWith("bump")) vel += (0.35 * 12) / PLANTS[kind].kV;
			}
		} else {
			for (const [time, g] of script.goals) if (t + 1e-9 >= time) goal = g;
			for (const s of script.shots ?? []) {
				if (Math.abs(s - t) < 0.01) {
					shotLeft = 0.08;
					shots++;
				}
			}
			if (script.payload !== undefined && t >= script.payload) payload = true;
		}

		const reported = kind === "steering" ? wrap(pos) : pos;
		const measuredVel = vel + noise();
		const output = kind === "flywheel" ? vel : reported;
		const measured = kind === "flywheel" ? measuredVel : reported;
		volts = Math.max(-12, Math.min(12, control(running, kind, goal, reported, measuredVel, t, state)));
		const estimate = script.noise && running !== "sysid" ? state.filtered : null;

		if (mode === "trial") {
			trace.time.push(t);
			trace.goal.push(goal);
			trace.output.push(output);
			trace.measured.push(measured);
			trace.volts.push(volts);
			trace.estimate.push(estimate);
		}

		// Move the mechanism forward 20 ms.
		const p = PLANTS[kind];
		for (let i = 0; i < 20; i++) {
			const h = 0.001;
			const gravity = kind === "elevator" ? p.kG + (payload ? 0.35 : 0) : kind === "arm" ? (p.kG + (payload ? 0.35 : 0)) * Math.cos(pos) : 0;
			const load = shotLeft > 0 ? p.shot * Math.sign(vel) : 0;
			shotLeft = Math.max(shotLeft - h, 0);
			const drive = volts - gravity - load - p.kV * vel;
			let next: number;
			if (vel === 0) next = Math.abs(drive) <= p.kS ? 0 : ((drive - Math.sign(drive) * p.kS) / p.kA) * h;
			else {
				next = vel + ((drive - Math.sign(vel) * p.kS) / p.kA) * h;
				if (Math.sign(next) !== Math.sign(vel)) next = 0;
			}
			vel = next;
			pos += vel * p.rate * h;
			if (pos <= p.min) [pos, vel] = [p.min, Math.max(vel, 0)];
			if (pos >= p.max) [pos, vel] = [p.max, Math.min(vel, 0)];
		}
		loop++;

		put(`${OUT}/Running`, "string", running);
		put(`${OUT}/Mode`, "string", mode);
		put(`${OUT}/RunId`, "string", runId);
		put(`${OUT}/Error`, "string", "");
		put(`${OUT}/Time`, "double", t);
		put(`${OUT}/Payload`, "boolean", payload);
		put(`${OUT}/Shots`, "int", shots);
		put(`${OUT}/PlotNames`, "string[]", []);
		put(`${OUT}/Sample`, "double[]", [t, goal, output, measured, volts, estimate ?? Number.NaN, reported, vel]);

		if (mode === "trial" && t + 0.02 >= script.duration - 1e-9) {
			const fails = running === "proportional";
			const result: TrialResult = {
				id: running,
				runId,
				passed: !fails,
				stars: fails ? 0 : 1,
				checks: [
					{ label: "Reaches every goal quickly", bonus: false, pass: true, detail: "settled in 0.32 s · settled in 0.28 s" },
					{ label: "Overshoots by less than a little", bonus: false, pass: !fails, detail: fails ? "overshot by 12%" : "no overshoot" },
					{ label: "Does it even faster", bonus: true, pass: !fails, detail: "settled in 0.18 s" },
				],
				fit:
					running === "sysid"
						? { ok: true, problem: "", kS: 0.3186, kV: 0.00175, kA: 0.0004176, samples: 377 }
						: undefined,
				trace: { ...structuredClone(trace), plots: {} },
			};
			put("/ControlLab/Result", "string", JSON.stringify(result));
			running = "";
			mode = "";
			put(`${OUT}/Running`, "string", "");
			put(`${OUT}/Mode`, "string", "");
			put(`${OUT}/RunId`, "string", "");
		}
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
	console.info("[coderunner] Not running inside CodeRunner: using an in-memory mock of the Control Lab.");
}
