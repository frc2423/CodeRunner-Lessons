import type { CatalogEntry, SysIdFit, TrialResult } from "../lab";

/** The trial's checks: ticked, crossed, or waiting for a run. */
export function Results({
	entry,
	result,
	running,
}: {
	entry: CatalogEntry | undefined;
	result: TrialResult | undefined;
	/** This challenge's trial is running right now. */
	running: boolean;
}) {
	const checks =
		result?.checks ??
		entry?.checks.map((check) => ({ ...check, pass: null as boolean | null, detail: "" })) ??
		[];
	const bonusCount = checks.filter((check) => check.bonus).length;
	const shown = running ? checks.map((check) => ({ ...check, pass: null, detail: "" })) : checks;

	return (
		<section className="card results">
			<h2>
				Trial checks
				{result && !running && (
					<span className={result.passed ? "badge badge-good" : "badge badge-bad"}>
						{result.passed ? "Challenge complete!" : "Not yet"}
					</span>
				)}
				{result && !running && bonusCount > 0 && (
					<span className="badge badge-star">
						{"★".repeat(result.stars)}
						{"☆".repeat(Math.max(bonusCount - result.stars, 0))} bonus
					</span>
				)}
			</h2>
			{shown.length === 0 ? (
				<p className="muted">Start the robot program to see what this trial checks.</p>
			) : (
				<ul className="checks">
					{shown.map((check) => (
						<li
							key={check.label}
							className={
								check.pass === null ? "check" : check.pass ? "check check-pass" : "check check-fail"
							}
						>
							<span className="check-icon" aria-hidden="true">
								{check.pass === null ? (check.bonus ? "☆" : "○") : check.pass ? (check.bonus ? "★" : "✓") : "✗"}
							</span>
							<span className="check-text">
								{check.bonus && <span className="bonus-tag">Bonus</span>}
								{check.label}
								{check.detail && <span className="check-detail">{check.detail}</span>}
							</span>
						</li>
					))}
				</ul>
			)}
			{running && <p className="muted">Trial running…</p>}
			{!running && !result && shown.length > 0 && (
				<p className="muted">Click Run trial to grade your controller.</p>
			)}
			{result?.fit && !running && <FitCard fit={result.fit} />}
		</section>
	);
}

function format(value: number | null): string {
	return value === null ? "?" : Number(value.toPrecision(3)).toString();
}

/** What SysId found, ready to paste into the next challenges. */
function FitCard({ fit }: { fit: SysIdFit }) {
	if (!fit.ok) {
		return (
			<div className="fit fit-failed">
				<strong>The lab couldn't fit a model.</strong> {fit.problem}
			</div>
		);
	}
	return (
		<div className="fit">
			<div className="fit-values">
				<span>
					kS <strong>{format(fit.kS)}</strong> V
				</span>
				<span>
					kV <strong>{format(fit.kV)}</strong> V/RPM
				</span>
				<span>
					kA <strong>{format(fit.kA)}</strong> V/(RPM/s)
				</span>
			</div>
			<p className="muted">
				Fitted from {fit.samples} samples. Paste these into LQR and Kalman Filter:
			</p>
			<pre className="fit-code">
				<code>
					{`private static final double kS = ${format(fit.kS)};\nprivate static final double kV = ${format(fit.kV)};\nprivate static final double kA = ${format(fit.kA)};`}
				</code>
			</pre>
		</div>
	);
}
