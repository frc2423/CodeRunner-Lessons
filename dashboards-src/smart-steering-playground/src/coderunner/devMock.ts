import { DEFAULT_CONFIG, type Rect } from "../playground";
import type { CodeRunnerApi, CodeRunnerContext, NTEntry, NTTopicInfo, Unsubscribe } from "./types";

/**
 * A stand-in `window.coderunner` for `bun run dev` outside CodeRunner.
 *
 * It keeps NetworkTables values in memory and plays a rough copy of
 * Playground.java: the same robot physics, the obstacles from the
 * lesson's starting Obstacles.java, and the starting SmartSteering.java (which
 * changes nothing). Drive it with the keyboard. Only installed in development,
 * and only when CodeRunner didn't inject the real API.
 *
 * In the browser console, `window.mockDisabled = true` pretends the robot is
 * disabled, and `window.mockError = "..."` pretends the steering code crashed.
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
	const flags = window as unknown as { mockDisabled?: boolean; mockError?: string };

	// The playground ----------------------------------------------------------------

	const C = DEFAULT_CONFIG;
	const DT = 0.02;
	const MAX_ACCEL = 8;
	const MAX_ANGULAR_ACCEL = 6 * Math.PI;
	const OUT = "/AdvantageKit/RealOutputs/Playground";
	const SD = "/SmartDashboard/Playground";
	put("/Playground/Config", "string", JSON.stringify(C));

	const fromCorners = (x1: number, y1: number, x2: number, y2: number): Rect => ({
		cx: (x1 + x2) / 2,
		cy: (y1 + y2) / 2,
		xw: Math.abs(x2 - x1),
		yw: Math.abs(y2 - y1),
		rot: 0,
	});
	const codeObstacles: Rect[] = [
		fromCorners(4.5, 1.5, 5.0, 6.5),
		fromCorners(8.0, 3.2, 9.2, 4.8),
		{ cx: 12, cy: 6, xw: 3, yw: 0.4, rot: (35 * Math.PI) / 180 },
		fromCorners(12.0, 0.0, 12.8, 2.6),
		fromCorners(12.0, 3.6, 12.8, 4.4),
	];
	let time = 0;
	let x = C.startX;
	let y = C.startY;
	let heading = C.startHeading;
	let vx = 0;
	let vy = 0;
	let omega = 0;
	let distance = 0;
	let resets = 1;
	let lastCommand = "";

	const reset = () => {
		x = C.startX;
		y = C.startY;
		heading = C.startHeading;
		vx = vy = omega = 0;
		distance = 0;
		resets++;
	};

	window.setInterval(() => {
		const command = get(`${SD}/Command`, "");
		if (command !== lastCommand) {
			lastCommand = command;
			if (command.startsWith("reset")) reset();
		}
		const keys = get<number[]>(`${SD}/Keys`, []);
		const smartOn = get(`${SD}/SmartSteering`, true);
		const drawnFlat = get<number[]>(`${SD}/DrawnObstacles`, []);
		const drawn: Rect[] = [];
		for (let i = 0; i + 5 <= drawnFlat.length; i += 5) {
			drawn.push({ cx: drawnFlat[i], cy: drawnFlat[i + 1], xw: drawnFlat[i + 2], yw: drawnFlat[i + 3], rot: drawnFlat[i + 4] });
		}
		const obstacles = [...codeObstacles, ...drawn];
		const driving = !flags.mockDisabled;

		// The sticks, like DriverInput.java.
		const shape = (v: number) => {
			const banded = Math.abs(v) < 0.08 ? 0 : (Math.abs(v) - 0.08) / 0.92;
			return banded * banded;
		};
		const sx = -(keys[1] ?? 0);
		const sy = -(keys[0] ?? 0);
		const mag = Math.min(1, Math.hypot(sx, sy));
		const scaled = shape(mag);
		const req = driving
			? {
					vx: mag > 0 ? (sx / Math.hypot(sx, sy)) * scaled * C.maxSpeed : 0,
					vy: mag > 0 ? (sy / Math.hypot(sx, sy)) * scaled * C.maxSpeed : 0,
					omega: -Math.sign(keys[4] ?? 0) * shape(Math.abs(keys[4] ?? 0)) * C.maxAngularSpeed,
				}
			: { vx: 0, vy: 0, omega: 0 };

		// The starting SmartSteering.java: changes nothing, shows one value.
		const steered = { ...req };

		// World.java
		time += DT;
		let dvx = steered.vx - vx;
		let dvy = steered.vy - vy;
		const dv = Math.hypot(dvx, dvy);
		if (dv > MAX_ACCEL * DT) {
			dvx *= (MAX_ACCEL * DT) / dv;
			dvy *= (MAX_ACCEL * DT) / dv;
		}
		vx += dvx;
		vy += dvy;
		omega += Math.max(-MAX_ANGULAR_ACCEL * DT, Math.min(MAX_ANGULAR_ACCEL * DT, steered.omega - omega));
		const x0 = x;
		const y0 = y;
		x += vx * DT;
		y += vy * DT;
		heading = Math.atan2(Math.sin(heading + omega * DT), Math.cos(heading + omega * DT));
		// Nothing is solid; the robot's center just stays on the field.
		const margin = C.robotWidth / 2;
		if (x < margin || x > C.fieldLength - margin) {
			x = Math.max(margin, Math.min(C.fieldLength - margin, x));
			vx = 0;
		}
		if (y < margin || y > C.fieldWidth - margin) {
			y = Math.max(margin, Math.min(C.fieldWidth - margin, y));
			vy = 0;
		}
		distance += Math.hypot(x - x0, y - y0);

		const error = flags.mockError ?? "";
		put(`${OUT}/Time`, "double", time);
		put(`${OUT}/State`, "double[]", [
			time, x, y, heading, vx, vy, omega, req.vx, req.vy, req.omega, steered.vx, steered.vy, steered.omega,
			driving ? 1 : 0, smartOn ? 1 : 0, smartOn ? 12 : 0, resets, distance, keys[0] ?? 0, keys[1] ?? 0, keys[4] ?? 0,
		]);
		put(
			`${OUT}/Obstacles`,
			"double[]",
			obstacles.flatMap((o, i) => [o.cx, o.cy, o.xw, o.yw, o.rot, i < codeObstacles.length ? 0 : 1]),
		);
		put(`${OUT}/Debug/Shapes`, "double[]", []);
		put(`${OUT}/Debug/Values`, "string[]", smartOn ? [`Requested speed (m/s)\t${Math.hypot(req.vx, req.vy).toFixed(3)}`] : []);
		put(`${OUT}/Error`, "string", smartOn ? error : "");
		put(`${OUT}/ObstacleError`, "string", "");
	}, DT * 1000);

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
	console.info("[coderunner] Not running inside CodeRunner: using an in-memory mock of the playground.");
}
