import { type PointerEvent, useCallback, useEffect, useRef, useState } from "react";
import {
	AXIS,
	BTN,
	EMPTY_PAD,
	isDrivingKey,
	KEY_FOR,
	keysToPad,
	mergePads,
	type PadState,
} from "../keyboard";
import type { ControllerState } from "../lab";

type Props = {
	controller: ControllerState;
	/** Sends this tab's controller (mouse + keyboard) to the robot. */
	onChange(values: number[]): void;
	/** Nothing is sent: no robot program, or a trial's script has the controller. */
	disabled: boolean;
};

const NEUTRAL = [0, -1, 0, 0, 0, 0, 0, 0];

/** Whether the user is typing into a form control, where keys aren't for driving. */
function typing(target: EventTarget | null): boolean {
	return target instanceof HTMLElement && (target.isContentEditable || /^(INPUT|SELECT|TEXTAREA)$/.test(target.tagName));
}

/** Whether a d-pad angle includes this direction (so diagonals light up both arrows). */
function povIncludes(pov: number, angle: number): boolean {
	if (pov < 0) return false;
	const diff = ((pov - angle + 540) % 360) - 180; // -180 to 180
	return Math.abs(diff) <= 45; // within 45° of the arrow's direction
}

/**
 * An Xbox controller showing what the robot sees: the Driver Station's
 * gamepad or keyboard, this tab's controller, or a trial's scripted driver.
 * Every control lights up while it's pressed, and so does its key hint.
 *
 * No real controller needed: click and hold its buttons, drag its sticks, or
 * use the keyboard while this tab has focus (the same keys as the Driver
 * Station's keyboard mode, drawn next to each control). With "Latch" on, a
 * click holds a button down until the next click.
 */
export function Gamepad({ controller, onChange, disabled }: Props) {
	const [mouse, setMouse] = useState<PadState>(EMPTY_PAD);
	const [keyboard, setKeyboard] = useState<PadState>(EMPTY_PAD);
	const [latch, setLatch] = useState(false);
	const [showKeys, setShowKeys] = useState(true);
	const [focused, setFocused] = useState(() => document.hasFocus());
	const mouseRef = useRef<PadState>(EMPTY_PAD);
	const keyboardRef = useRef<PadState>(EMPTY_PAD);
	const keys = useRef(new Set<string>());
	const disabledRef = useRef(disabled);
	disabledRef.current = disabled;

	const send = useCallback(() => {
		if (disabledRef.current) {
			onChange(NEUTRAL);
			return;
		}
		const merged = mergePads(mouseRef.current, keyboardRef.current);
		onChange([merged.buttons, merged.pov, ...merged.axes]);
	}, [onChange]);
	const setMousePad = useCallback(
		(next: PadState) => {
			mouseRef.current = next;
			setMouse(next);
			send();
		},
		[send],
	);
	const setKeyboardPad = useCallback(
		(next: PadState) => {
			keyboardRef.current = next;
			setKeyboard(next);
			send();
		},
		[send],
	);

	// The keyboard drives the robot while this tab has focus. Keys are tracked
	// even with no robot running, so the controller still lights up for them.
	useEffect(() => {
		const update = () => setKeyboardPad(keysToPad(keys.current));
		const down = (event: KeyboardEvent) => {
			if (typing(event.target) || event.ctrlKey || event.metaKey || event.altKey) return;
			if (!isDrivingKey(event.code)) return;
			event.preventDefault(); // arrow keys would scroll the page
			if (keys.current.has(event.code)) return;
			keys.current.add(event.code);
			update();
		};
		const up = (event: KeyboardEvent) => {
			if (!keys.current.delete(event.code)) return;
			update();
		};
		const focus = () => setFocused(true);
		const blur = () => {
			setFocused(false);
			keys.current.clear();
			update();
		};
		window.addEventListener("keydown", down);
		window.addEventListener("keyup", up);
		window.addEventListener("focus", focus);
		window.addEventListener("blur", blur);
		return () => {
			window.removeEventListener("keydown", down);
			window.removeEventListener("keyup", up);
			window.removeEventListener("focus", focus);
			window.removeEventListener("blur", blur);
		};
	}, [setKeyboardPad]);

	// Let go of everything when the tab closes; resend when sending is allowed again.
	useEffect(() => () => onChange(NEUTRAL), [onChange]);
	useEffect(() => {
		// `disabled` is read through disabledRef; this re-sends whenever it changes.
		void disabled;
		send();
	}, [disabled, send]);

	const local = mergePads(mouse, keyboard);
	const scripted = controller.source === "script";
	// During a trial, show only what the script presses.
	const shownButtons = scripted ? controller.buttons : controller.buttons | local.buttons;
	const shownAxes = scripted
		? controller.axes
		: controller.axes.map((value, i) => (local.axes[i] !== 0 ? local.axes[i] : value));
	const shownPov = scripted || local.pov < 0 ? controller.pov : local.pov;
	const down = (button: number) => (shownButtons & (1 << (button - 1))) !== 0;

	const press = (button: number, pressed: boolean) => {
		const bit = 1 << (button - 1);
		const v = mouseRef.current;
		setMousePad({ ...v, buttons: pressed ? v.buttons | bit : v.buttons & ~bit });
	};
	const setAxis = (index: number, value: number) => {
		const v = mouseRef.current;
		const axes = v.axes.slice();
		axes[index] = value;
		setMousePad({ ...v, axes });
	};
	const setPov = (angle: number) => setMousePad({ ...mouseRef.current, pov: angle });

	/** Pointer handlers for something that's held while the pointer is down. */
	const hold = (isDown: () => boolean, set: (pressed: boolean) => void) => ({
		onPointerDown: (event: PointerEvent<SVGElement>) => {
			if (scripted) return;
			event.preventDefault();
			if (latch) {
				set(!isDown());
				return;
			}
			event.currentTarget.setPointerCapture(event.pointerId);
			set(true);
		},
		onPointerUp: () => {
			if (!latch) set(false);
		},
		onPointerCancel: () => {
			if (!latch) set(false);
		},
	});
	const button = (b: number) =>
		hold(
			() => (mouseRef.current.buttons & (1 << (b - 1))) !== 0,
			(pressed) => press(b, pressed),
		);
	const trigger = (index: number) =>
		hold(
			() => mouseRef.current.axes[index] > 0.5,
			(pressed) => setAxis(index, pressed ? 1 : 0),
		);
	const dpad = (angle: number) =>
		hold(
			() => mouseRef.current.pov === angle,
			(pressed) => setPov(pressed ? angle : -1),
		);

	const stick = (axisX: number, axisY: number, cx: number, cy: number) => {
		const move = (event: PointerEvent<SVGElement>) => {
			const svg = event.currentTarget.ownerSVGElement;
			if (!svg) return;
			const point = svg.createSVGPoint();
			point.x = event.clientX;
			point.y = event.clientY;
			const at = point.matrixTransform(svg.getScreenCTM()?.inverse());
			let x = (at.x - cx) / 18;
			let y = (at.y - cy) / 18;
			const length = Math.hypot(x, y);
			if (length > 1) {
				x /= length;
				y /= length;
			}
			const v = mouseRef.current;
			const axes = v.axes.slice();
			axes[axisX] = Math.round(x * 100) / 100;
			axes[axisY] = Math.round(y * 100) / 100;
			setMousePad({ ...v, axes });
		};
		const release = () => {
			const v = mouseRef.current;
			const axes = v.axes.slice();
			axes[axisX] = 0;
			axes[axisY] = 0;
			setMousePad({ ...v, axes });
		};
		return {
			onPointerDown: (event: PointerEvent<SVGElement>) => {
				if (scripted) return;
				event.preventDefault();
				event.currentTarget.setPointerCapture(event.pointerId);
				move(event);
			},
			onPointerMove: (event: PointerEvent<SVGElement>) => {
				if (event.currentTarget.hasPointerCapture(event.pointerId)) move(event);
			},
			onPointerUp: release,
			onPointerCancel: release,
		};
	};

	/** A key hint: lit up while its control is active. */
	const hint = (text: string, x: number, y: number, active: boolean) =>
		showKeys ? (
			<g className={active ? "pad-key pad-key-on" : "pad-key"} pointerEvents="none">
				<rect x={x - 7} y={y - 7} width={14} height={14} rx={3} />
				<text x={x} y={y + 3.5} textAnchor="middle">
					{text}
				</text>
			</g>
		) : null;

	const face = (b: number, label: string, key: string, x: number, y: number, color: string) => (
		<g className={down(b) ? "pad-face pad-down" : "pad-face"} style={{ color }} {...button(b)}>
			<circle cx={x} cy={y} r={12} />
			<text x={x} y={y + 4.5} textAnchor="middle">
				{label}
			</text>
			{hint(key, x + 21, y, down(b))}
		</g>
	);

	/** A stick, with its four keys around it, each lit while the stick leans that way. */
	const knob = (x: number, y: number, ax: number, ay: number, label: string, keyLabels: string[]) => {
		const sx = shownAxes[ax];
		const sy = shownAxes[ay];
		const moved = Math.hypot(sx, sy) > 0.05;
		const [up, left, downKey, right] = keyLabels;
		return (
			<g>
				<g className={moved ? "pad-stick pad-down" : "pad-stick"} {...stick(ax, ay, x, y)}>
					<circle className="pad-stick-base" cx={x} cy={y} r={27} />
					<circle className="pad-stick-knob" cx={x + sx * 15} cy={y + sy * 15} r={14} />
				</g>
				{hint(up, x, y - 37, sy < -0.3)}
				{hint(downKey, x, y + 37, sy > 0.3)}
				{hint(left, x - 37, y, sx < -0.3)}
				{hint(right, x + 37, y, sx > 0.3)}
				{!showKeys && (
					<text className="pad-small" x={x} y={y + 42} textAnchor="middle">
						{label}
					</text>
				)}
			</g>
		);
	};

	const triggerBar = (index: number, label: string, key: string, x: number) => {
		const on = shownAxes[index] > 0.5;
		return (
			<g className={on ? "pad-trigger pad-down" : "pad-trigger"} {...trigger(index)}>
				<rect x={x} y={6} width={64} height={18} rx={7} />
				<rect
					className="pad-trigger-fill"
					x={x}
					y={6}
					width={64 * Math.max(0, Math.min(1, shownAxes[index]))}
					height={18}
					rx={7}
				/>
				<text x={x + 32} y={19} textAnchor="middle">
					{label}
				</text>
				{hint(key, x + (x < 160 ? -11 : 75), 15, on)}
			</g>
		);
	};
	const bumper = (b: number, label: string, key: string, x: number) => (
		<g className={down(b) ? "pad-bumper pad-down" : "pad-bumper"} {...button(b)}>
			<rect x={x} y={30} width={64} height={16} rx={7} />
			<text x={x + 32} y={42} textAnchor="middle">
				{label}
			</text>
			{hint(key, x + (x < 160 ? -11 : 75), 38, down(b))}
		</g>
	);
	const arrow = (angle: number, x: number, y: number, w: number, h: number) => (
		<rect
			className={povIncludes(shownPov, angle) ? "pad-dpad pad-down" : "pad-dpad"}
			x={x}
			y={y}
			width={w}
			height={h}
			rx={3}
			{...dpad(angle)}
		/>
	);
	const small = (b: number, x: number, key: string) => (
		<g className={down(b) ? "pad-small-button pad-down" : "pad-small-button"} {...button(b)}>
			<rect x={x} y={104} width={20} height={11} rx={5.5} />
			{hint(key, x + 10, 92, down(b))}
		</g>
	);

	// Where the d-pad sits.
	const dx = 118;
	const dy = 168;
	const anythingHeld = mouse.buttons !== 0 || mouse.pov >= 0 || mouse.axes.some((a) => a !== 0);

	return (
		<div className={scripted ? "gamepad gamepad-scripted" : "gamepad"}>
			<svg viewBox="0 0 320 252" role="img" aria-label="Controller">
				{/* An Xbox-shaped body with grips. */}
				<path
					className="pad-body"
					d="M 82 58 Q 160 46 238 58 Q 292 66 306 132 Q 318 206 292 236 Q 262 260 236 222 L 216 200 L 104 200 L 84 222 Q 58 260 28 236 Q 2 206 14 132 Q 28 66 82 58 Z"
				/>
				{triggerBar(AXIS.LT, "LT", KEY_FOR.LT, 46)}
				{triggerBar(AXIS.RT, "RT", KEY_FOR.RT, 210)}
				{bumper(BTN.LB, "LB", KEY_FOR.LB, 46)}
				{bumper(BTN.RB, "RB", KEY_FOR.RB, 210)}
				{knob(78, 116, AXIS.LX, AXIS.LY, "move", ["W", "A", "S", "D"])}
				{knob(206, 168, AXIS.RX, AXIS.RY, "turn", ["↑", "←", "↓", "→"])}
				{arrow(0, dx - 7, dy - 23, 14, 16)}
				{arrow(180, dx - 7, dy + 7, 14, 16)}
				{arrow(270, dx - 23, dy - 7, 16, 14)}
				{arrow(90, dx + 7, dy - 7, 16, 14)}
				{hint(KEY_FOR.POV_UP, dx, dy - 32, povIncludes(shownPov, 0))}
				{hint(KEY_FOR.POV_DOWN, dx, dy + 32, povIncludes(shownPov, 180))}
				{hint(KEY_FOR.POV_LEFT, dx - 32, dy, povIncludes(shownPov, 270))}
				{hint(KEY_FOR.POV_RIGHT, dx + 32, dy, povIncludes(shownPov, 90))}
				{small(BTN.BACK, 134, KEY_FOR.BACK)}
				{small(BTN.START, 166, KEY_FOR.START)}
				{face(BTN.Y, "Y", KEY_FOR.Y, 244, 90, "#e7b416")}
				{face(BTN.X, "X", KEY_FOR.X, 218, 116, "#3b82f6")}
				{face(BTN.B, "B", KEY_FOR.B, 270, 116, "#e5484d")}
				{face(BTN.A, "A", KEY_FOR.A, 244, 142, "#2fbf71")}
			</svg>
			<div className="gamepad-footer">
				{scripted ? (
					<span className="tag">Scripted driver (trial)</span>
				) : focused ? (
					<span className="tag tag-good" title="Keys drive the robot while this tab has focus">
						⌨ Keyboard on · Shift = slow
					</span>
				) : (
					<span className="tag tag-plain">Click this tab to drive with the keyboard</span>
				)}
				{controller.rumble > 0 && <span className="tag tag-rumble">rumble</span>}
				<label className="pad-option" title="Show which key presses each control">
					<input type="checkbox" checked={showKeys} onChange={(event) => setShowKeys(event.target.checked)} />
					Keys
				</label>
				<label className="pad-option" title="Clicks hold buttons down until you click again">
					<input type="checkbox" checked={latch} onChange={(event) => setLatch(event.target.checked)} />
					Latch
				</label>
				{anythingHeld && (
					<button type="button" className="button small" onClick={() => setMousePad(EMPTY_PAD)}>
						Release
					</button>
				)}
			</div>
		</div>
	);
}
