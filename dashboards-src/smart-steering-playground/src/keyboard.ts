import { useEffect, useRef, useState } from "react";

/**
 * Driving with the keyboard while this tab has focus. It uses the same keys as
 * the CodeRunner Driver Station's keyboard mode, so they work the same
 * whichever of the two has focus:
 *
 * - W A S D: left stick (drive)
 * - ← →: right stick, left and right (spin)
 * - Shift (held): half-speed sticks
 */

const DRIVING_KEYS = new Set([
	"KeyW",
	"KeyA",
	"KeyS",
	"KeyD",
	"ArrowUp",
	"ArrowDown",
	"ArrowLeft",
	"ArrowRight",
	"ShiftLeft",
	"ShiftRight",
]);

const NEUTRAL = [0, 0, 0, 0, 0, 0];

function axis(keys: ReadonlySet<string>, negative: string, positive: string): number {
	const n = keys.has(negative);
	const p = keys.has(positive);
	return n === p ? 0 : n ? -1 : 1;
}

/** Six controller axes, in WPILib's XboxController.Axis order, for these held keys. */
export function keysToAxes(keys: ReadonlySet<string>): number[] {
	const scale = keys.has("ShiftLeft") || keys.has("ShiftRight") ? 0.5 : 1;
	return [
		axis(keys, "KeyA", "KeyD") * scale,
		axis(keys, "KeyW", "KeyS") * scale,
		0,
		0,
		axis(keys, "ArrowLeft", "ArrowRight") * scale,
		axis(keys, "ArrowUp", "ArrowDown") * scale,
	];
}

/** Whether the user is typing into a form control, where keys aren't for driving. */
function typing(target: EventTarget | null): boolean {
	return target instanceof HTMLElement && (target.isContentEditable || /^(INPUT|SELECT|TEXTAREA)$/.test(target.tagName));
}

/**
 * Sends the keyboard to the robot while this tab has focus. Returns whether
 * the tab has focus and which driving keys are held, for the on-screen hints.
 */
export function useKeyboardDriving(send: (axes: number[]) => void): { focused: boolean; held: ReadonlySet<string> } {
	const [focused, setFocused] = useState(() => document.hasFocus());
	const [held, setHeld] = useState<ReadonlySet<string>>(new Set());
	const keys = useRef(new Set<string>());
	const sendRef = useRef(send);
	sendRef.current = send;

	useEffect(() => {
		const update = () => {
			setHeld(new Set(keys.current));
			sendRef.current(keysToAxes(keys.current));
		};
		const down = (event: KeyboardEvent) => {
			if (!DRIVING_KEYS.has(event.code) || typing(event.target)) return;
			// Arrow keys would otherwise scroll the page.
			event.preventDefault();
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
			sendRef.current(NEUTRAL);
		};
	}, []);

	return { focused, held };
}
