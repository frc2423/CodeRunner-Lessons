import { useEffect, useRef } from "react";
import type { ButtonRef } from "../challenges";

const BITS: Record<ButtonRef["name"], number> = { A: 1, B: 2, X: 4, Y: 8 };
/** The same keys as the Driver Station's keyboard mode. */
const KEYS: Record<ButtonRef["name"], string> = { A: "k", B: "l", X: "j", Y: "i" };

/** Axis numbers, in `XboxController.Axis` order. */
const LEFT_X = 0;
const LEFT_Y = 1;
const RIGHT_X = 4;

/**
 * Just the buttons this challenge binds, as clickable buttons and keys. In a
 * trial they show what the scripted driver is pressing.
 */
export function Controls({
	buttons,
	sticks,
	piece,
	bits,
	active,
	note,
	hasPiece,
	onChange,
	onPiece,
}: {
	buttons: ButtonRef[];
	sticks: boolean;
	piece: boolean;
	/** What's pressed on the robot right now. */
	bits: number;
	/** The student can press things right now. */
	active: boolean;
	note: string;
	hasPiece: boolean;
	onChange: (bits: number, axes: number[]) => void;
	onPiece: () => void;
}) {
	const pressed = useRef(0);
	const keys = useRef(new Set<string>());
	const onChangeRef = useRef(onChange);
	onChangeRef.current = onChange;

	const send = () => {
		const down = keys.current;
		let keyBits = 0;
		for (const button of buttons) if (down.has(KEYS[button.name])) keyBits |= BITS[button.name];
		const axes = [0, 0, 0, 0, 0, 0];
		if (sticks) {
			axes[LEFT_Y] = (down.has("s") ? 1 : 0) - (down.has("w") ? 1 : 0);
			axes[LEFT_X] = (down.has("d") ? 1 : 0) - (down.has("a") ? 1 : 0);
			axes[RIGHT_X] = (down.has("arrowright") ? 1 : 0) - (down.has("arrowleft") ? 1 : 0);
		}
		onChangeRef.current(pressed.current | keyBits, axes);
	};
	useEffect(() => {
		if (!active) return;
		const down = (event: KeyboardEvent) => {
			if (event.target instanceof HTMLInputElement || event.target instanceof HTMLSelectElement) return;
			const key = event.key.toLowerCase();
			if (event.repeat || keys.current.has(key)) return;
			keys.current.add(key);
			send();
		};
		const up = (event: KeyboardEvent) => {
			keys.current.delete(event.key.toLowerCase());
			send();
		};
		const clear = () => {
			keys.current.clear();
			pressed.current = 0;
			send();
		};
		window.addEventListener("keydown", down);
		window.addEventListener("keyup", up);
		window.addEventListener("blur", clear);
		return () => {
			window.removeEventListener("keydown", down);
			window.removeEventListener("keyup", up);
			window.removeEventListener("blur", clear);
			clear();
		};
	}, [active, buttons, sticks]);

	const press = (bit: number, isDown: boolean) => {
		pressed.current = isDown ? pressed.current | bit : pressed.current & ~bit;
		send();
	};

	return (
		<section className="card panel">
			<span className="panel-label">Driver</span>
			<div className="controls-row">
				{buttons.map((button) => {
					const bit = BITS[button.name];
					return (
						<div key={button.name} className="control">
							<button
								type="button"
								className={`pad-button pad-${button.name.toLowerCase()}${bits & bit ? " pad-on" : ""}`}
								disabled={!active}
								onPointerDown={(event) => {
									event.currentTarget.setPointerCapture(event.pointerId);
									press(bit, true);
								}}
								onPointerUp={() => press(bit, false)}
								onPointerCancel={() => press(bit, false)}
							>
								{button.name}
							</button>
							<span className="control-text">
								{button.does}
								<kbd>{KEYS[button.name].toUpperCase()}</kbd>
							</span>
						</div>
					);
				})}
				{sticks && (
					<span className="control-text">
						<kbd>W</kbd>
						<kbd>A</kbd>
						<kbd>S</kbd>
						<kbd>D</kbd> drive, <kbd>←</kbd>
						<kbd>→</kbd> turn
					</span>
				)}
				{piece && (
					<div className="control">
						<button type="button" className="button" disabled={!active || hasPiece} onClick={onPiece}>
							+ Piece
						</button>
						<span className="control-text">{hasPiece ? "There's a piece in the feeder" : "The feeder is empty"}</span>
					</div>
				)}
			</div>
			{note && <p className="muted">{note}</p>}
		</section>
	);
}
