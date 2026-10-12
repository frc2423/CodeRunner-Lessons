/**
 * The keyboard layout for driving without a controller. It is the same as the
 * CodeRunner Driver Station's keyboard mode, so the keys work the same whether
 * this tab or the Driver Station has focus.
 */

/** A controller's state: button bits (A = bit 0), d-pad angle, six axes. */
export type PadState = { buttons: number; pov: number; axes: number[] };

export const EMPTY_PAD: PadState = { buttons: 0, pov: -1, axes: [0, 0, 0, 0, 0, 0] };

/** Button numbers, as in WPILib's XboxController.Button (bit = number - 1). */
export const BTN = { A: 1, B: 2, X: 3, Y: 4, LB: 5, RB: 6, BACK: 7, START: 8, LSTICK: 9, RSTICK: 10 };

/** Axis numbers, as in WPILib's XboxController.Axis. */
export const AXIS = { LX: 0, LY: 1, LT: 2, RT: 3, RX: 4, RY: 5 };

/** Which key drives each control, for the hints drawn on the controller. */
export const KEY_FOR = {
	A: "K",
	B: "L",
	X: "J",
	Y: "I",
	LB: "E",
	RB: "U",
	LT: "Q",
	RT: "O",
	BACK: "R",
	START: "Y",
	LSTICK: "F",
	RSTICK: "H",
	POV_UP: "Z",
	POV_DOWN: "X",
	POV_LEFT: "C",
	POV_RIGHT: "V",
	MOVE: "W A S D",
	TURN: "← ↑ ↓ →",
};

const BUTTON_KEYS: Record<string, number> = {
	KeyK: BTN.A,
	KeyL: BTN.B,
	KeyJ: BTN.X,
	KeyI: BTN.Y,
	KeyE: BTN.LB,
	KeyU: BTN.RB,
	KeyR: BTN.BACK,
	KeyY: BTN.START,
	KeyF: BTN.LSTICK,
	KeyH: BTN.RSTICK,
};

const OTHER_KEYS = new Set([
	"KeyW",
	"KeyA",
	"KeyS",
	"KeyD",
	"ArrowUp",
	"ArrowDown",
	"ArrowLeft",
	"ArrowRight",
	"KeyQ",
	"KeyO",
	"KeyZ",
	"KeyX",
	"KeyC",
	"KeyV",
	"ShiftLeft",
	"ShiftRight",
]);

export function isDrivingKey(code: string): boolean {
	return code in BUTTON_KEYS || OTHER_KEYS.has(code);
}

function axis(keys: ReadonlySet<string>, negative: string, positive: string): number {
	const n = keys.has(negative);
	const p = keys.has(positive);
	return n === p ? 0 : n ? -1 : 1;
}

function pov(up: boolean, down: boolean, left: boolean, right: boolean): number {
	if (up && right) return 45;
	if (right && down) return 135;
	if (down && left) return 225;
	if (left && up) return 315;
	if (up) return 0;
	if (right) return 90;
	if (down) return 180;
	if (left) return 270;
	return -1;
}

/** The controller state for these held keys. Shift halves the sticks, for gentle driving. */
export function keysToPad(keys: ReadonlySet<string>): PadState {
	let buttons = 0;
	for (const [code, button] of Object.entries(BUTTON_KEYS)) {
		if (keys.has(code)) buttons |= 1 << (button - 1);
	}
	const scale = keys.has("ShiftLeft") || keys.has("ShiftRight") ? 0.5 : 1;
	return {
		buttons,
		pov: pov(keys.has("KeyZ"), keys.has("KeyX"), keys.has("KeyC"), keys.has("KeyV")),
		axes: [
			axis(keys, "KeyA", "KeyD") * scale,
			axis(keys, "KeyW", "KeyS") * scale,
			keys.has("KeyQ") ? 1 : 0,
			keys.has("KeyO") ? 1 : 0,
			axis(keys, "ArrowLeft", "ArrowRight") * scale,
			axis(keys, "ArrowUp", "ArrowDown") * scale,
		],
	};
}

/** Two controllers at once: buttons add up, and a moved stick wins over a still one. */
export function mergePads(a: PadState, b: PadState): PadState {
	return {
		buttons: a.buttons | b.buttons,
		pov: a.pov >= 0 ? a.pov : b.pov,
		axes: a.axes.map((value, i) => (value !== 0 ? value : b.axes[i])),
	};
}
