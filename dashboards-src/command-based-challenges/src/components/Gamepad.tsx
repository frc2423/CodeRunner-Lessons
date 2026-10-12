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
	disabled: boolean;
};

/** Whether the user is typing into a form control, where keys aren't for driving. */
function typing(target: EventTarget | null): boolean {
	return target instanceof HTMLElement && (target.isContentEditable || /^(INPUT|SELECT|TEXTAREA)$/.test(target.tagName));
}

/**
 * An Xbox controller showing what the robot sees: the Driver Station's
 * gamepad or keyboard, this tab's controller, or a trial's scripted driver.
 *
 * No real controller needed: click and hold its buttons, drag its sticks, or
 * use the keyboard while this tab has focus (the same keys as the Driver
 * Station's keyboard mode, drawn next to each button). With "Latch" on, a
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

	const send = useCallback(() => {
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

	// The keyboard drives the robot while this tab has focus.
	useEffect(() => {
		const update = () => setKeyboardPad(keysToPad(keys.current));
		const down = (event: KeyboardEvent) => {
			if (disabled || typing(event.target) || event.ctrlKey || event.metaKey || event.altKey) return;
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
		const releaseAll = () => {
			keys.current.clear();
			update();
		};
		const focus = () => setFocused(true);
		const blur = () => {
			setFocused(false);
			releaseAll();
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
	}, [disabled, setKeyboardPad]);

	// Let go of everything when the tab closes or the trial's script takes over.
	useEffect(() => () => onChange([0, -1, 0, 0, 0, 0, 0, 0]), [onChange]);
	useEffect(() => {
		if (!disabled) return;
		keys.current.clear();
		if (mouseRef.current !== EMPTY_PAD) setMousePad(EMPTY_PAD);
		if (keyboardRef.current !== EMPTY_PAD) setKeyboardPad(EMPTY_PAD);
	}, [disabled, setMousePad, setKeyboardPad]);

	const local = mergePads(mouse, keyboard);
	const scripted = controller.source === "script";
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
			if (disabled) return;
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
			let x = (at.x - cx) / 16;
			let y = (at.y - cy) / 16;
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
				if (disabled) return;
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

	const hint = (text: string, x: number, y: number, anchor: "start" | "middle" | "end" = "middle") =>
		showKeys ? (
			<text className="pad-key" x={x} y={y} textAnchor={anchor}>
				{text}
			</text>
		) : null;
	const face = (b: number, label: string, key: string, x: number, y: number, color: string) => (
		<g className={down(b) ? "pad-face pad-down" : "pad-face"} {...button(b)}>
			<circle cx={x} cy={y} r={11} style={{ fill: down(b) ? color : undefined, stroke: color }} />
			<text x={x} y={y + 4} textAnchor="middle" style={{ fill: down(b) ? "#fff" : color }}>
				{label}
			</text>
			{hint(key, x + 15, y + 4, "start")}
		</g>
	);
	const knob = (x: number, y: number, ax: number, ay: number, label: string, key: string) => (
		<g className="pad-stick" {...stick(ax, ay, x, y)}>
			<circle className="pad-stick-base" cx={x} cy={y} r={24} />
			<circle className="pad-stick-knob" cx={x + shownAxes[ax] * 14} cy={y + shownAxes[ay] * 14} r={12} />
			<text className="pad-small" x={x} y={y + 36} textAnchor="middle">
				{label}
				{showKeys ? `: ${key}` : ""}
			</text>
		</g>
	);
	const triggerBar = (index: number, label: string, key: string, x: number) => (
		<g className={shownAxes[index] > 0.5 ? "pad-trigger pad-down" : "pad-trigger"} {...trigger(index)}>
			<rect x={x} y={6} width={52} height={14} rx={6} />
			<rect
				className="pad-trigger-fill"
				x={x}
				y={6}
				width={52 * Math.max(0, Math.min(1, shownAxes[index]))}
				height={14}
				rx={6}
			/>
			<text x={x + 26} y={17} textAnchor="middle">
				{label}
				{showKeys ? ` · ${key}` : ""}
			</text>
		</g>
	);
	const bumper = (b: number, label: string, key: string, x: number) => (
		<g className={down(b) ? "pad-bumper pad-down" : "pad-bumper"} {...button(b)}>
			<rect x={x} y={26} width={52} height={14} rx={6} />
			<text x={x + 26} y={37} textAnchor="middle">
				{label}
				{showKeys ? ` · ${key}` : ""}
			</text>
		</g>
	);
	const arrow = (angle: number, x: number, y: number, w: number, h: number) => (
		<rect
			className={shownPov === angle ? "pad-dpad pad-down" : "pad-dpad"}
			x={x}
			y={y}
			width={w}
			height={h}
			rx={3}
			{...dpad(angle)}
		/>
	);

	const anythingHeld =
		mouse.buttons !== 0 || mouse.pov >= 0 || mouse.axes.some((a) => a !== 0);

	return (
		<div className={disabled ? "gamepad gamepad-disabled" : "gamepad"}>
			<svg viewBox="0 0 320 196" role="img" aria-label="Controller">
				<path
					className="pad-body"
					d="M 70 48 Q 160 36 250 48 Q 300 56 308 120 Q 314 176 280 184 Q 252 190 232 160 L 210 140 L 110 140 L 88 160 Q 68 190 40 184 Q 6 176 12 120 Q 20 56 70 48 Z"
				/>
				{triggerBar(AXIS.LT, "LT", KEY_FOR.LT, 44)}
				{triggerBar(AXIS.RT, "RT", KEY_FOR.RT, 224)}
				{bumper(BTN.LB, "LB", KEY_FOR.LB, 44)}
				{bumper(BTN.RB, "RB", KEY_FOR.RB, 224)}
				{knob(80, 90, AXIS.LX, AXIS.LY, "move", KEY_FOR.MOVE)}
				{knob(206, 136, AXIS.RX, AXIS.RY, "turn", KEY_FOR.TURN)}
				{arrow(0, 122, 112, 14, 16)}
				{arrow(180, 122, 142, 14, 16)}
				{arrow(270, 104, 128, 16, 14)}
				{arrow(90, 138, 128, 16, 14)}
				{hint(KEY_FOR.POV_UP, 129, 108)}
				{hint(KEY_FOR.POV_DOWN, 129, 170)}
				{hint(KEY_FOR.POV_LEFT, 98, 139, "end")}
				{hint(KEY_FOR.POV_RIGHT, 158, 139, "start")}
				<g className={down(BTN.BACK) ? "pad-small-button pad-down" : "pad-small-button"} {...button(BTN.BACK)}>
					<rect x={136} y={78} width={18} height={10} rx={5} />
					{hint(KEY_FOR.BACK, 145, 74)}
				</g>
				<g className={down(BTN.START) ? "pad-small-button pad-down" : "pad-small-button"} {...button(BTN.START)}>
					<rect x={166} y={78} width={18} height={10} rx={5} />
					{hint(KEY_FOR.START, 175, 74)}
				</g>
				{face(BTN.Y, "Y", KEY_FOR.Y, 246, 66, "#e7b416")}
				{face(BTN.X, "X", KEY_FOR.X, 222, 90, "#3b82f6")}
				{face(BTN.B, "B", KEY_FOR.B, 270, 90, "#e5484d")}
				{face(BTN.A, "A", KEY_FOR.A, 246, 114, "#2fbf71")}
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
				<label className="pad-option" title="Show which key presses each button">
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
