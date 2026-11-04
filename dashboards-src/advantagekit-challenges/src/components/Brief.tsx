import type { Challenge } from "../challenges";
import type { CatalogEntry, TrialResult } from "../lab";
import { Inline } from "./Inline";

type Props = {
	challenge: Challenge;
	entry: CatalogEntry | undefined;
	result: TrialResult | undefined;
	/** This challenge's trial is waiting, running or replaying. */
	trialActive: boolean;
	clock: { now: number; duration: number } | null;
	/** What the trial is waiting for, or what's going on. */
	message: string;
};

const fileName = (path: string) => path.slice(path.lastIndexOf("/") + 1);

/**
 * The challenge at a glance, always at the top: what to do, which files, what
 * the trial does, and every check.
 */
export function Brief({ challenge, entry, result, trialActive, clock, message }: Props) {
	const checks =
		result && !trialActive
			? result.checks
			: (entry?.checks ?? []).map((check) => ({ ...check, pass: null as boolean | null, detail: "" }));
	const bonusCount = checks.filter((check) => check.bonus).length;

	return (
		<section className="card brief">
			<div className="brief-head">
				<h1 className="brief-title">
					{challenge.number > 0 && `${challenge.number}. `}
					{challenge.title}
				</h1>
				{challenge.files.map((file) => (
					<code key={file} className="brief-file" title={file}>
						{fileName(file)}
					</code>
				))}
				{result && !trialActive && (
					<span className={result.passed ? "badge badge-good" : "badge badge-bad"}>
						{result.passed ? "Complete!" : "Not yet"}
					</span>
				)}
				{result && !trialActive && bonusCount > 0 && (
					<span className="badge badge-star">
						{"★".repeat(result.stars)}
						{"☆".repeat(Math.max(bonusCount - result.stars, 0))}
					</span>
				)}
			</div>
			<p className="brief-summary">
				<Inline text={challenge.summary} />
			</p>
			<p className="brief-trial">
				<strong>The trial:</strong> <Inline text={challenge.trial} />
				{clock && (
					<span className="trial-clock">
						<span className="trial-clock-bar">
							<span style={{ width: `${Math.min(clock.now / Math.max(clock.duration, 0.01), 1) * 100}%` }} />
						</span>
						{clock.now.toFixed(1)} / {clock.duration.toFixed(0)} s
					</span>
				)}
			</p>
			{message && (
				<p className="brief-waiting" role="status">
					<Inline text={message} />
				</p>
			)}
			{checks.length > 0 && (
				<ul className="checks">
					{checks.map((check) => (
						<li
							key={check.label}
							className={check.pass === null ? "check" : check.pass ? "check check-pass" : "check check-fail"}
							title={check.detail || undefined}
						>
							<span className="check-icon" aria-hidden="true">
								{check.pass === null ? (check.bonus ? "☆" : "○") : check.pass ? (check.bonus ? "★" : "✓") : "✗"}
							</span>
							<span className="check-text">
								{check.bonus && <span className="bonus-tag">Bonus</span>}
								{check.label}
								{/* Failures say why right away; passes keep their detail for the tooltip. */}
								{check.detail && check.pass === false && <span className="check-detail">{check.detail}</span>}
							</span>
						</li>
					))}
				</ul>
			)}
		</section>
	);
}
