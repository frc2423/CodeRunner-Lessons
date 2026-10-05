import { type PointerEvent, useCallback, useEffect, useRef, useState } from "react";
import type { ControllerState } from "../lab";

/** Button numbers, as in WPILib's XboxController.Button (bit = number - 1). */
const BTN = { A: 1, B: 2, X: 3, Y: 4, LB: 5, RB: 6, BACK: 7, START: 8 };
/** Axis numbers, as in WPILib's XboxController.Axis. */
const AXIS = { LX: 0, LY: 1, LT: 2, RT: 3, RX: 4, RY: 5 };

type Virtual = { buttons: number; pov: number; axes: number[] };
const EMPTY: Virtual = { buttons: 0, pov: -1, axes: [0, 0, 0, 0, 0, 0] };

type Props = {
	controller: ControllerState;
	/** Sends the on-screen controller's state to the robot. */
	onChange(values: number[]): void;
	disabled: boolean;
};

/**
 * An Xbox controller showing what the robot sees: the Driver Station's
 * gamepad or keyboard, this on-screen one, or a trial's scripted driver. Click
 * and hold its buttons, and drag its sticks, to drive the robot with the mouse.
 * With "Latch" on, a click holds a button down until the next click.
 */
export function Gamepad({ controller, onChange, disabled }: Props) {
	const [virtual, setVirtual] = useState<Virtual>(EMPTY);
	const [latch, setLatch] = useState(false);
	const current = useRef<Virtual>(EMPTY);

	const send = useCallback(
		(next: Virtual) => {
			current.current = next;
			setVirtual(next);
			onChange([next.buttons, next.pov, ...next.axes]);
		},
		[onChange],
	);

	// Let go of everything when the tab closes or the trial's script takes over.
	useEffect(() => () => onChange([0, -1, 0, 0, 0, 0, 0, 0]), [onChange]);
	useEffect(() => {
		if (disabled && current.current !== EMPTY) send(EMPTY);
	}, [disabled, send]);

	const scripted = controller.source === "script";
	const shownButtons = scripted ? controller.buttons : controller.buttons | virtual.buttons;
	const shownAxes = scripted
		? controller.axes
		: controller.axes.map((value, i) => (virtual.axes[i] !== 0 ? virtual.axes[i] : value));
	const shownPov = scripted || virtual.pov < 0 ? controller.pov : virtual.pov;
	const down = (button: number) => (shownButtons & (1 << (button - 1))) !== 0;

	const press = (button: number, pressed: boolean) => {
		const bit = 1 << (button - 1);
		const v = current.current;
		send({ ...v, buttons: pressed ? v.buttons | bit : v.buttons & ~bit });
	};
	const setAxis = (axis: number, value: number) => {
		const v = current.current;
		const axes = v.axes.slice();
		axes[axis] = value;
		send({ ...v, axes });
	};
	const setPov = (pov: number) => send({ ...current.current, pov });

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
			() => (current.current.buttons & (1 << (b - 1))) !== 0,
			(pressed) => press(b, pressed),
		);
	const trigger = (axis: number) =>
		hold(
			() => current.current.axes[axis] > 0.5,
			(pressed) => setAxis(axis, pressed ? 1 : 0),
		);
	const dpad = (angle: number) =>
		hold(
			() => current.current.pov === angle,
			(pressed) => setPov(pressed ? angle : -1),
		);

	const stick = (axisX: number, axisY: number, cx: number, cy: number) => {
		const move = (event: PointerEvent<SVGElement>) => {
			const svg = event.currentTarget.ownerSVGElement;
			if (!svg) return;
			const point = svg.createSVGPoint();
			point.x = event.clientX;
			point.y = event.clientY;
			const local = point.matrixTransform(svg.getScreenCTM()?.inverse());
			let x = (local.x - cx) / 16;
			let y = (local.y - cy) / 16;
			const length = Math.hypot(x, y);
			if (length > 1) {
				x /= length;
				y /= length;
			}
			const v = current.current;
			const axes = v.axes.slice();
			axes[axisX] = Math.round(x * 100) / 100;
			axes[axisY] = Math.round(y * 100) / 100;
			send({ ...v, axes });
		};
		const release = () => {
			const v = current.current;
			const axes = v.axes.slice();
			axes[axisX] = 0;
			axes[axisY] = 0;
			send({ ...v, axes });
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

	const face = (b: number, label: string, x: number, y: number, color: string) => (
		<g className={down(b) ? "pad-face pad-down" : "pad-face"} {...button(b)}>
			<circle cx={x} cy={y} r={11} style={{ fill: down(b) ? color : undefined, stroke: color }} />
			<text x={x} y={y + 4} textAnchor="middle" style={{ fill: down(b) ? "#fff" : color }}>
				{label}
			</text>
		</g>
	);
	const knob = (x: number, y: number, ax: number, ay: number, label: string) => (
		<g className="pad-stick" {...stick(ax, ay, x, y)}>
			<circle className="pad-stick-base" cx={x} cy={y} r={24} />
			<circle className="pad-stick-knob" cx={x + shownAxes[ax] * 14} cy={y + shownAxes[ay] * 14} r={12} />
			<text className="pad-small" x={x} y={y + 40} textAnchor="middle">
				{label}
			</text>
		</g>
	);
	const triggerBar = (axis: number, label: string, x: number) => (
		<g className={shownAxes[axis] > 0.5 ? "pad-trigger pad-down" : "pad-trigger"} {...trigger(axis)}>
			<rect x={x} y={6} width={52} height={14} rx={6} />
			<rect className="pad-trigger-fill" x={x} y={6} width={52 * Math.max(0, Math.min(1, shownAxes[axis]))} height={14} rx={6} />
			<text x={x + 26} y={17} textAnchor="middle">
				{label}
			</text>
		</g>
	);
	const bumper = (b: number, label: string, x: number) => (
		<g className={down(b) ? "pad-bumper pad-down" : "pad-bumper"} {...button(b)}>
			<rect x={x} y={26} width={52} height={14} rx={6} />
			<text x={x + 26} y={37} textAnchor="middle">
				{label}
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

	return (
		<div className={disabled ? "gamepad gamepad-disabled" : "gamepad"}>
			<svg viewBox="0 0 320 200" role="img" aria-label="Controller">
				<path
					className="pad-body"
					d="M 70 48 Q 160 36 250 48 Q 300 56 308 120 Q 314 176 280 184 Q 252 190 232 160 L 210 140 L 110 140 L 88 160 Q 68 190 40 184 Q 6 176 12 120 Q 20 56 70 48 Z"
				/>
				{triggerBar(AXIS.LT, "LT", 44)}
				{triggerBar(AXIS.RT, "RT", 224)}
				{bumper(BTN.LB, "LB", 44)}
				{bumper(BTN.RB, "RB", 224)}
				{knob(80, 90, AXIS.LX, AXIS.LY, "move")}
				{knob(206, 136, AXIS.RX, AXIS.RY, "turn")}
				{arrow(0, 122, 112, 14, 16)}
				{arrow(180, 122, 142, 14, 16)}
				{arrow(270, 104, 128, 16, 14)}
				{arrow(90, 138, 128, 16, 14)}
				<g className={down(BTN.BACK) ? "pad-small-button pad-down" : "pad-small-button"} {...button(BTN.BACK)}>
					<rect x={136} y={78} width={18} height={10} rx={5} />
				</g>
				<g className={down(BTN.START) ? "pad-small-button pad-down" : "pad-small-button"} {...button(BTN.START)}>
					<rect x={166} y={78} width={18} height={10} rx={5} />
				</g>
				{face(BTN.Y, "Y", 246, 66, "#e7b416")}
				{face(BTN.X, "X", 222, 90, "#3b82f6")}
				{face(BTN.B, "B", 270, 90, "#e5484d")}
				{face(BTN.A, "A", 246, 114, "#2fbf71")}
			</svg>
			<div className="gamepad-footer">
				<span className={scripted ? "tag" : "tag tag-plain"}>
					{scripted ? "Scripted driver (trial)" : "Your controller"}
				</span>
				{controller.rumble > 0 && <span className="tag tag-rumble">rumble</span>}
				<label className="latch" title="Clicks hold buttons down until you click again">
					<input type="checkbox" checked={latch} onChange={(event) => setLatch(event.target.checked)} />
					Latch buttons
				</label>
				{(virtual.buttons !== 0 || virtual.pov >= 0 || virtual.axes.some((a) => a !== 0)) && (
					<button type="button" className="button small" onClick={() => send(EMPTY)}>
						Release all
					</button>
				)}
			</div>
		</div>
	);
}
