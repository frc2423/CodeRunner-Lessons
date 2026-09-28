import type { Challenge } from "../challenges";
import { unpack } from "../leds";

/**
 * Ticks off each of a challenge's checks against the robot's LEDs. Checks are
 * only judged while that challenge is the one running.
 */
export function Checklist({
	challenge,
	colors,
	active,
}: {
	challenge: Challenge;
	colors: readonly number[] | undefined;
	/** The selected challenge is the one running on the robot. */
	active: boolean;
}) {
	const checks = challenge.checks;
	if (!checks) return null;

	const leds = active && colors && colors.length > 0 ? colors.map(unpack) : null;
	const results = checks.map((check) => (leds ? check.pass(leds) : null));
	const allPassed = results.every((result) => result === true);

	return (
		<div className="checklist">
			<h3>
				Checks
				{allPassed && <span className="badge badge-good">Challenge complete!</span>}
			</h3>
			<ul>
				{checks.map((check, i) => {
					const result = results[i];
					return (
						<li
							key={check.label}
							className={
								result === null ? "check" : result ? "check check-pass" : "check check-fail"
							}
						>
							<span className="check-icon" aria-hidden="true">
								{result === null ? "○" : result ? "✓" : "✗"}
							</span>
							{check.label}
						</li>
					);
				})}
			</ul>
			{!leds && <p className="muted">Run this challenge to check your LEDs.</p>}
		</div>
	);
}
