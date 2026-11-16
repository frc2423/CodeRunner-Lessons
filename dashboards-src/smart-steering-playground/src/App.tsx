import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useCodeRunnerContext } from "./coderunner/hooks";
import { FieldView, type Layers } from "./components/FieldView";
import { useKeyboardDriving } from "./keyboard";
import { type DrawnObstacle, loadJson, type Rect, type RobotState, type Speeds, saveJson, usePlayground } from "./playground";

const DRAWN_KEY = "smart-steering:drawn";
const SMART_KEY = "smart-steering:on";
const LAYERS_KEY = "smart-steering:layers";
const MAX_DRAWN = 100;
const TRAIL_POINTS = 400;

function validObstacle(o: unknown): o is DrawnObstacle {
	const r = o as DrawnObstacle;
	return (
		typeof r?.id === "string" &&
		[r.cx, r.cy, r.xw, r.yw, r.rot].every((v) => typeof v === "number" && Number.isFinite(v)) &&
		r.xw > 0 &&
		r.yw > 0
	);
}

let nextId = Date.now();
const newId = () => `o${(nextId++).toString(36)}`;

/** Recent robot positions, cleared whenever the robot is reset. */
function useTrail(state: RobotState | undefined): [number, number][] {
	const trail = useRef<{ resets: number; points: [number, number][] }>({ resets: -1, points: [] });
	return useMemo(() => {
		const t = trail.current;
		if (!state) return t.points;
		if (state.resets !== t.resets) {
			t.resets = state.resets;
			t.points = [];
		}
		const last = t.points[t.points.length - 1];
		if (!last || Math.hypot(last[0] - state.x, last[1] - state.y) > 0.03) {
			t.points = [...t.points.slice(-(TRAIL_POINTS - 1)), [state.x, state.y]];
		}
		return t.points;
	}, [state]);
}

const n = (v: number, digits = 2) => (Object.is(Math.round(v * 100), -0) ? 0 : v).toFixed(digits);
const deg = (rad: number) => Math.round((rad * 180) / Math.PI);

/** Java for the drawn obstacles, to paste into Obstacles.java. */
function toJava(drawn: DrawnObstacle[]): string {
	const lines = drawn.map((o) => {
		if (Math.abs(o.rot) < 1e-9) {
			return `new Rectangle2d(new Translation2d(${n(o.cx - o.xw / 2)}, ${n(o.cy - o.yw / 2)}), new Translation2d(${n(o.cx + o.xw / 2)}, ${n(o.cy + o.yw / 2)}))`;
		}
		return `new Rectangle2d(new Pose2d(${n(o.cx)}, ${n(o.cy)}, Rotation2d.fromDegrees(${deg(o.rot)})), ${n(o.xw)}, ${n(o.yw)})`;
	});
	return `${lines.map((line) => `        ${line}`).join(",\n")}`;
}

export default function App() {
	const context = useCodeRunnerContext();

	// Follow CodeRunner's light/dark theme.
	useEffect(() => {
		document.documentElement.dataset.theme = context?.theme ?? "dark";
	}, [context?.theme]);

	const [drawn, setDrawn] = useState<DrawnObstacle[]>(() =>
		loadJson<unknown[]>(DRAWN_KEY, []).filter(validObstacle).slice(0, MAX_DRAWN),
	);
	const [smartOn, setSmartOn] = useState(() => loadJson<boolean>(SMART_KEY, true) !== false);
	const [layers, setLayers] = useState<Layers>(() => ({
		arrows: true,
		trail: true,
		debug: true,
		...loadJson<Partial<Layers>>(LAYERS_KEY, {}),
	}));
	const [selectedId, setSelectedId] = useState<string | null>(null);
	const [showCode, setShowCode] = useState(false);

	useEffect(() => saveJson(DRAWN_KEY, drawn), [drawn]);
	useEffect(() => saveJson(SMART_KEY, smartOn), [smartOn]);
	useEffect(() => saveJson(LAYERS_KEY, layers), [layers]);

	const pg = usePlayground(drawn, smartOn);
	const { state, config } = pg;
	const keyboard = useKeyboardDriving(pg.setKeys);
	const trail = useTrail(state);
	const selected = drawn.find((o) => o.id === selectedId) ?? null;

	const add = useCallback((rect: Rect) => {
		const id = newId();
		setDrawn((list) => (list.length >= MAX_DRAWN ? list : [...list, { ...rect, id }]));
		setSelectedId(id);
	}, []);
	const moveTo = useCallback((id: string, cx: number, cy: number) => {
		setDrawn((list) => list.map((o) => (o.id === id ? { ...o, cx, cy } : o)));
	}, []);
	const rotate = (degrees: number) => {
		if (!selected) return;
		setDrawn((list) =>
			list.map((o) => {
				if (o.id !== selected.id) return o;
				let rot = o.rot + (degrees * Math.PI) / 180;
				rot = Math.atan2(Math.sin(rot), Math.cos(rot));
				if (Math.abs(rot) < 1e-6) rot = 0;
				return { ...o, rot };
			}),
		);
	};
	const remove = useCallback((id: string) => {
		setDrawn((list) => list.filter((o) => o.id !== id));
		setSelectedId(null);
	}, []);

	// Delete or Backspace removes the selected obstacle; Escape deselects it.
	useEffect(() => {
		const onKey = (event: KeyboardEvent) => {
			if (event.target instanceof HTMLElement && /^(INPUT|TEXTAREA|SELECT)$/.test(event.target.tagName)) return;
			if (!selectedId) return;
			if (event.key === "Delete" || event.key === "Backspace") {
				event.preventDefault();
				remove(selectedId);
			} else if (event.key === "Escape") {
				setSelectedId(null);
			}
		};
		window.addEventListener("keydown", onKey);
		return () => window.removeEventListener("keydown", onKey);
	}, [selectedId, remove]);

	const status = !pg.connected
		? { text: "No robot program", className: "tag tag-plain" }
		: pg.stalled
			? { text: "Not responding", className: "tag tag-bad" }
			: state?.driving
				? { text: "Driving", className: "tag tag-good" }
				: { text: "Disabled", className: "tag tag-warn" };

	return (
		<div className="app">
			<header className="toolbar">
				<span className={pg.connected && !pg.stalled ? "dot dot-on" : "dot"} />
				<h1>Smart Steering Playground</h1>
				<span className={status.className}>{status.text}</span>
				<div className="spacer" />
				<label className="switch" title="Turn your SmartSteering code on or off, to compare">
					<input type="checkbox" checked={smartOn} onChange={(e) => setSmartOn(e.target.checked)} />
					<span className="switch-track" />
					Smart steering {smartOn ? "on" : "off"}
				</label>
				<button type="button" className="button" onClick={pg.reset} disabled={!pg.connected}>
					Reset robot
				</button>
			</header>

			{!pg.connected && (
				<div className="banner">
					Click <b>Start</b> in the Driver Station to run the robot program.
				</div>
			)}
			{pg.stalled && (
				<div className="banner banner-error">
					The robot program stopped responding. <code>SmartSteering.steer</code> is probably stuck in a loop that never
					ends: fix it, then click <b>Restart</b> in the Driver Station.
				</div>
			)}
			{pg.connected && !pg.stalled && state && !state.driving && (
				<div className="banner banner-warn">
					Set the Driver Station to <b>Teleop</b> and click <b>Enable</b> to drive.
				</div>
			)}
			{pg.error && (
				<div className="banner banner-error">
					<b>Your steering code crashed</b>, so the robot is driving without it.
					<code className="error-text">{pg.error}</code>
				</div>
			)}
			{pg.obstacleError && (
				<div className="banner banner-error">
					<b>Obstacles.java crashed</b>, so its obstacles are missing.
					<code className="error-text">{pg.obstacleError}</code>
				</div>
			)}

			<div className="field-card">
				<FieldView
					config={config}
					state={state}
					codeObstacles={pg.codeObstacles}
					drawn={drawn}
					drawnInside={pg.drawnInside}
					selectedId={selectedId}
					onSelect={setSelectedId}
					onAdd={add}
					onMove={moveTo}
					shapes={pg.shapes}
					bumps={pg.bumps}
					trail={trail}
					layers={layers}
				/>
				<div className="field-tools">
					{selected ? (
						<>
							<span className="tools-label">
								Selected obstacle: {n(selected.xw)} × {n(selected.yw)} m at ({n(selected.cx)}, {n(selected.cy)})
								{selected.rot !== 0 && `, ${deg(selected.rot)}°`}
							</span>
							<button type="button" className="button button-small" onClick={() => rotate(15)} title="Rotate 15° counterclockwise">
								⟲ 15°
							</button>
							<button type="button" className="button button-small" onClick={() => rotate(-15)} title="Rotate 15° clockwise">
								⟳ 15°
							</button>
							<button type="button" className="button button-small button-danger" onClick={() => remove(selected.id)}>
								Delete
							</button>
							<button type="button" className="button button-small" onClick={() => setSelectedId(null)}>
								Done
							</button>
						</>
					) : (
						<span className="tools-label muted">
							Drag on the field to draw an obstacle. Drag one you drew to move it, or click it to rotate or delete it.
						</span>
					)}
					<div className="spacer" />
					<button type="button" className="button button-small" onClick={() => setShowCode(!showCode)} disabled={drawn.length === 0}>
						Copy as code
					</button>
					<button
						type="button"
						className="button button-small"
						onClick={() => {
							setDrawn([]);
							setSelectedId(null);
						}}
						disabled={drawn.length === 0}
					>
						Clear drawn ({drawn.length})
					</button>
				</div>
				<div className="field-tools legend">
					<Legend className="legend-requested" label="Driver asked for" shown={smartOn} />
					<Legend className="legend-steered" label={smartOn ? "Smart steering drives" : "Robot drives"} shown />
					<span className="legend-item">
						<span className="swatch swatch-code" /> Obstacles.java
					</span>
					<span className="legend-item">
						<span className="swatch swatch-drawn" /> Drawn here
					</span>
					<div className="spacer" />
					<Toggle label="Speed arrows" checked={layers.arrows} onChange={(arrows) => setLayers({ ...layers, arrows })} />
					<Toggle label="Trail" checked={layers.trail} onChange={(trail) => setLayers({ ...layers, trail })} />
					<Toggle label="Debug drawings" checked={layers.debug} onChange={(debug) => setLayers({ ...layers, debug })} />
				</div>
				{showCode && drawn.length > 0 && <CodePanel code={toJava(drawn)} onClose={() => setShowCode(false)} />}
			</div>

			<div className="cards">
				<section className="card">
					<h2>Drive</h2>
					<div className="keys">
						<span className="key-group">
							<KeyCap code="KeyW" held={keyboard.held}>
								W
							</KeyCap>
							<KeyCap code="KeyA" held={keyboard.held}>
								A
							</KeyCap>
							<KeyCap code="KeyS" held={keyboard.held}>
								S
							</KeyCap>
							<KeyCap code="KeyD" held={keyboard.held}>
								D
							</KeyCap>
							<span className="keys-label">drive</span>
						</span>
						<span className="key-group">
							<KeyCap code="ArrowLeft" held={keyboard.held}>
								←
							</KeyCap>
							<KeyCap code="ArrowRight" held={keyboard.held}>
								→
							</KeyCap>
							<span className="keys-label">spin</span>
						</span>
						<span className="key-group">
							<KeyCap code="ShiftLeft" held={keyboard.held}>
								Shift
							</KeyCap>
							<span className="keys-label">slow</span>
						</span>
					</div>
					<p className="muted">
						{keyboard.focused ? (
							<span className="tag tag-good">Keys drive the robot</span>
						) : (
							<span className="tag tag-plain">Click this tab to drive with the keys</span>
						)}{" "}
						Or plug in a gamepad and pick it in the Driver Station's <b>Controls</b>: left stick drives, right stick spins.
						Driving is field-relative: up on the stick (or W) drives away from the blue driver station.
					</p>
				</section>

				<section className="card">
					<h2>Speeds</h2>
					<table className="speeds">
						<thead>
							<tr>
								<th />
								<th>vx</th>
								<th>vy</th>
								<th>speed</th>
								<th>ω</th>
							</tr>
						</thead>
						<tbody>
							<SpeedRow label="Driver asked for" className="legend-requested" speeds={state?.requested} />
							<SpeedRow
								label="steer() returned"
								className="legend-steered"
								speeds={state?.smartOn ? state.steered : undefined}
							/>
							<SpeedRow label="Robot is doing" speeds={state?.actual} />
						</tbody>
					</table>
					<p className="muted">m/s and rad/s, field-relative.</p>
				</section>

				<section className="card">
					<h2>Bumps</h2>
					<div className="stats">
						<Stat label="Bumps" value={state ? String(state.bumps) : "–"} warn={(state?.bumps ?? 0) > 0} big />
						<Stat label="Hardest bump" value={state ? `${n(state.hardestBump, 1)} m/s` : "–"} />
						<Stat label="Inside obstacles" value={state ? `${n(state.insideTime, 1)} s` : "–"} />
						<Stat label="Distance driven" value={state ? `${n(state.distance, 1)} m` : "–"} />
						<Stat
							label="steer() took"
							value={state?.smartOn ? `${state.steerMicros < 1000 ? Math.round(state.steerMicros) : n(state.steerMicros / 1000, 1)} ${state.steerMicros < 1000 ? "µs" : "ms"}` : "–"}
						/>
					</div>
					<p className="muted">
						Obstacles aren't solid: the robot drives right through them, and each time it drives into one counts as a
						bump. The field walls are solid; hitting one faster than 0.25 m/s counts too. <b>Reset robot</b> clears the
						count.
					</p>
				</section>

				<section className="card">
					<h2>Debug values</h2>
					{pg.values.length === 0 ? (
						<p className="muted">
							Call <code>Debug.value("name", value)</code> in <code>SmartSteering.java</code> to show numbers here.
						</p>
					) : (
						<dl className="values">
							{pg.values.map(([name, value]) => (
								<div key={name}>
									<dt>{name}</dt>
									<dd>{value}</dd>
								</div>
							))}
						</dl>
					)}
				</section>
			</div>

			<details className="card about">
				<summary>
					<h2>What to do</h2>
				</summary>
				<p>
					Open <code>src/main/java/frc/robot/SmartSteering.java</code>. Every 20 ms the robot calls its{" "}
					<code>steer</code> method with the speeds the driver asked for, where the robot is, how fast it's going, and
					every obstacle (including the field walls) as a <code>Rectangle2d</code>. Whatever it returns is what the robot
					really drives at. Make it smart: the driver drives, but the robot never ends up in an obstacle. Obstacles
					aren't solid, so when your steering doesn't work the robot drives straight through them and they light up red.
				</p>
				<ol>
					<li>
						<b>Look ahead.</b> Where will the robot be in a moment? Draw it with <code>Debug.rectangle</code> and check it
						with <code>Geometry.overlaps</code>.
					</li>
					<li>
						<b>Brake</b> before you'd hit something.
					</li>
					<li>
						<b>Slide.</b> Only remove the part of the speed that points <i>into</i> the obstacle. Drive diagonally into a
						wall: the robot should glide along it.
					</li>
					<li>
						<b>Be smooth.</b> From <code>d</code> meters away, the fastest speed you can still stop from is{" "}
						<code>Math.sqrt(2 * MAX_ACCELERATION * d)</code>.
					</li>
					<li>
						<b>Watch the corners.</b> Spinning swings the robot's corners out. And the robot can't change speed instantly:
						use <code>currentSpeeds</code>.
					</li>
					<li>
						<b>Steer.</b> Like in Mario Kart, gently turn the robot around obstacles in its way.
					</li>
				</ol>
				<p className="muted">
					Add obstacles in <code>Obstacles.java</code>, or draw them here and use <b>Copy as code</b> to keep them. Save
					and click <b>Restart</b> in the Driver Station after changing code; the obstacles you drew stay.
				</p>
			</details>
		</div>
	);
}

function Legend({ className, label, shown }: { className: string; label: string; shown: boolean }) {
	if (!shown) return null;
	return (
		<span className="legend-item">
			<span className={`legend-arrow ${className}`} /> {label}
		</span>
	);
}

function Toggle({ label, checked, onChange }: { label: string; checked: boolean; onChange(value: boolean): void }) {
	return (
		<label className="toggle">
			<input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} /> {label}
		</label>
	);
}

function KeyCap({ code, held, children }: { code: string; held: ReadonlySet<string>; children: string }) {
	const down = held.has(code) || (code === "ShiftLeft" && held.has("ShiftRight"));
	return <kbd className={down ? "key key-down" : "key"}>{children}</kbd>;
}

function SpeedRow({ label, className, speeds }: { label: string; className?: string; speeds: Speeds | undefined }) {
	return (
		<tr>
			<th>
				{className && <span className={`legend-arrow ${className}`} />} {label}
			</th>
			<td>{speeds ? n(speeds.vx) : "–"}</td>
			<td>{speeds ? n(speeds.vy) : "–"}</td>
			<td>{speeds ? n(Math.hypot(speeds.vx, speeds.vy)) : "–"}</td>
			<td>{speeds ? n(speeds.omega) : "–"}</td>
		</tr>
	);
}

function Stat({ label, value, warn = false, big = false }: { label: string; value: string; warn?: boolean; big?: boolean }) {
	return (
		<div className={big ? "stat stat-big" : "stat"}>
			<span className={warn ? "stat-value stat-warn" : "stat-value"}>{value}</span>
			<span className="stat-label">{label}</span>
		</div>
	);
}

/** The drawn obstacles as Java. The clipboard may be blocked in a dashboard, so the text is also selectable. */
function CodePanel({ code, onClose }: { code: string; onClose(): void }) {
	const area = useRef<HTMLTextAreaElement>(null);
	const [copied, setCopied] = useState("");
	const copy = async () => {
		area.current?.select();
		try {
			await navigator.clipboard.writeText(code);
			setCopied("Copied!");
		} catch {
			setCopied(document.execCommand?.("copy") ? "Copied!" : "Press Ctrl+C (⌘C on a Mac) to copy.");
		}
	};
	return (
		<div className="code-panel">
			<p className="muted">
				Paste these into the <code>List.of(...)</code> in <code>Obstacles.java</code> (add a comma after the line before
				them). Then you can clear the drawn ones.
			</p>
			<textarea ref={area} readOnly value={code} rows={Math.min(10, code.split("\n").length + 1)} onFocus={(e) => e.target.select()} />
			<div className="code-actions">
				<button type="button" className="button button-small button-primary" onClick={copy}>
					Copy
				</button>
				<span className="muted">{copied}</span>
				<div className="spacer" />
				<button type="button" className="button button-small" onClick={onClose}>
					Close
				</button>
			</div>
		</div>
	);
}
