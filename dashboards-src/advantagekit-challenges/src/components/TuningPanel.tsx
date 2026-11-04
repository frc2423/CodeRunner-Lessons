import { useState } from "react";

/** The drive team's dashboard: a target speed, and a shot preset. */
export function TuningPanel({
	active,
	onTuning,
	onPreset,
}: {
	active: boolean;
	onTuning: (rpm: number) => void;
	onPreset: (name: string) => void;
}) {
	const [rpm, setRpm] = useState(3000);
	const [preset, setPreset] = useState("Near");

	return (
		<section className="card panel">
			<span className="panel-label">The drive team's dashboard</span>
			<div className="tuning">
				<label className="tuning-row">
					<span>
						Target speed <strong>{rpm} RPM</strong>
					</span>
					<input
						type="range"
						min={1000}
						max={5000}
						step={100}
						value={rpm}
						disabled={!active}
						onChange={(event) => {
							const next = Number(event.target.value);
							setRpm(next);
							onTuning(next);
						}}
					/>
				</label>
				<label className="tuning-row">
					<span>Shot preset (bonus)</span>
					<select
						value={preset}
						disabled={!active}
						onChange={(event) => {
							setPreset(event.target.value);
							onPreset(event.target.value);
						}}
					>
						<option>Near</option>
						<option>Far</option>
					</select>
				</label>
			</div>
			{!active && <p className="muted">Use these in free play. During a trial, the script changes the values.</p>}
		</section>
	);
}
