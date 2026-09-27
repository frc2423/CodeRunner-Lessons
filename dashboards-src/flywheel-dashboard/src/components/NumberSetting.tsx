import { useEffect, useState } from "react";
import { useNTValue } from "../coderunner/hooks";

/**
 * A number the dashboard publishes for the robot to read, e.g. with
 * `SmartDashboard.getNumber("SpeedScale", 1.0)` in robot code.
 */
export function NumberSetting({
	topic,
	label,
	min,
	max,
	step,
	defaultValue,
	digits = 2,
}: {
	topic: string;
	label: string;
	min: number;
	max: number;
	step: number;
	defaultValue: number;
	/** Decimal places shown next to the label. */
	digits?: number;
}) {
	const [value, setValue] = useNTValue<number>(topic, defaultValue);
	// Keep the slider responsive while dragging; publish as it moves.
	const [draft, setDraft] = useState(value);
	useEffect(() => setDraft(value), [value]);

	return (
		<label className="field">
			<span className="field-label">
				{label} <output>{draft.toFixed(digits)}</output>
			</span>
			<input
				type="range"
				min={min}
				max={max}
				step={step}
				value={draft}
				onChange={(event) => {
					const next = Number(event.target.value);
					setDraft(next);
					setValue(next, "double");
				}}
			/>
		</label>
	);
}
