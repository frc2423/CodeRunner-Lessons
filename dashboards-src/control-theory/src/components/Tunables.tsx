import { useState } from "react";
import type { Slider } from "../challenges";
import { useNTValue } from "../coderunner/hooks";
import { forgetTunable, rememberTunable, tunableTopic } from "../lab";

const LOG_STEPS = 1000;

function toSlider(slider: Slider, value: number): number {
	if (!slider.log) return value;
	const ratio = Math.log(value / slider.min) / Math.log(slider.max / slider.min);
	return Math.round(Math.min(Math.max(ratio, 0), 1) * LOG_STEPS);
}

function fromSlider(slider: Slider, position: number): number {
	if (!slider.log) return position;
	const value = slider.min * (slider.max / slider.min) ** (position / LOG_STEPS);
	// Keep log sliders to three significant figures.
	return Number(value.toPrecision(3));
}

/** Shows a number without float noise, like 0.0021 instead of 0.0021000000000000003. */
function tidy(value: number): string {
	return Number(value.toPrecision(6)).toString();
}

function TunableRow({
	challengeId,
	name,
	defaultValue,
	slider,
	disabled,
}: {
	challengeId: string;
	name: string;
	defaultValue: number;
	slider: Slider | undefined;
	disabled: boolean;
}) {
	const [value, setValue] = useNTValue<number>(
		tunableTopic(challengeId, name),
		defaultValue,
	);
	// What the student is typing, until it's a valid number.
	const [draft, setDraft] = useState<string | null>(null);

	const set = (next: number) => {
		if (!Number.isFinite(next)) return;
		setValue(next, "double");
		rememberTunable(challengeId, name, next);
	};

	return (
		<div className="tunable">
			<label className="tunable-name" htmlFor={`tunable-${name}`}>
				{name}
				{slider?.unit && <span className="muted"> {slider.unit}</span>}
			</label>
			{slider ? (
				<input
					type="range"
					aria-label={`${name} slider`}
					min={slider.log ? 0 : slider.min}
					max={slider.log ? LOG_STEPS : slider.max}
					step={slider.log ? 1 : slider.step}
					value={toSlider(slider, value)}
					disabled={disabled}
					onChange={(event) => set(fromSlider(slider, Number(event.target.value)))}
				/>
			) : (
				<span className="muted">no slider: type a value</span>
			)}
			<input
				id={`tunable-${name}`}
				className="tunable-input"
				type="text"
				inputMode="decimal"
				value={draft ?? tidy(value)}
				disabled={disabled}
				onChange={(event) => {
					setDraft(event.target.value);
					const parsed = Number(event.target.value);
					if (event.target.value.trim() !== "" && Number.isFinite(parsed)) set(parsed);
				}}
				onBlur={() => setDraft(null)}
			/>
			<button
				type="button"
				className="button icon-button small"
				title={`Back to the default in your code (${tidy(defaultValue)})`}
				aria-label={`Reset ${name}`}
				disabled={disabled || value === defaultValue}
				onClick={() => {
					setValue(defaultValue, "double");
					forgetTunable(challengeId, name);
					setDraft(null);
				}}
			>
				↺
			</button>
		</div>
	);
}

/**
 * A slider for each TunableNumber the challenge's class makes. Values go
 * straight to the robot, so they take effect on the next loop.
 */
export function Tunables({
	challengeId,
	tunables,
	sliders,
	connected,
}: {
	challengeId: string;
	/** From the robot's catalog: the TunableNumbers this challenge's class makes. */
	tunables: { name: string; default: number }[] | undefined;
	sliders: Record<string, Slider> | undefined;
	connected: boolean;
}) {
	if (!connected) {
		return <p className="muted">Start the robot program to tune this challenge.</p>;
	}
	if (!tunables || tunables.length === 0) {
		return <p className="muted">This challenge has no tunable numbers.</p>;
	}
	return (
		<div className="tunables">
			{tunables.map((tunable) => (
				<TunableRow
					key={`${challengeId}/${tunable.name}`}
					challengeId={challengeId}
					name={tunable.name}
					defaultValue={tunable.default}
					slider={sliders?.[tunable.name]}
					disabled={!connected}
				/>
			))}
			<p className="muted">
				Sliders change the robot right away. Anything your code reads in <code>reset</code> needs a new run.
			</p>
		</div>
	);
}
