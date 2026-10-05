import { useState } from "react";
import type { Challenge } from "../challenges";
import { Inline } from "./Inline";

/** A path that wraps after slashes rather than mid-word. */
function FilePath({ file }: { file: string }) {
	return (
		<code>
			{file.split("/").map((part, i) => (
				<span key={`${i}-${part}`}>
					{i > 0 && "/"}
					{i > 0 && <wbr />}
					{part}
				</span>
			))}
		</code>
	);
}

/**
 * Everything a student needs to attempt a challenge: the idea behind it, where
 * to work, what the trial does, useful code, hints revealed one at a time, and
 * further reading.
 *
 * Render it with `key={challenge.id}` so the revealed hints reset.
 */
export function ChallengeDetails({ challenge }: { challenge: Challenge }) {
	const [hintsShown, setHintsShown] = useState(0);

	return (
		<article className="card details">
			<header className="details-header">
				<span className="level">
					{challenge.level}
					{challenge.mode === "auto" && <span className="tag">Autonomous</span>}
				</span>
				<h2 className="details-title">
					{challenge.number > 0 && `${challenge.number}. `}
					{challenge.title}
				</h2>
				<p className="summary">
					<Inline text={challenge.summary} />
				</p>
			</header>

			<section>
				<h3>The idea</h3>
				{challenge.idea.map((paragraph) => (
					<p key={paragraph} className="idea">
						<Inline text={paragraph} />
					</p>
				))}
			</section>

			<section>
				<h3>{challenge.files.length > 1 ? "Edit these files" : "Edit this file"}</h3>
				{challenge.files.map((file) => (
					<p key={file} className="file-path">
						<FilePath file={file} />
					</p>
				))}
			</section>

			<section>
				<h3>You'll practise</h3>
				<ul className="chips">
					{challenge.learn.map((topic) => (
						<li key={topic}>{topic}</li>
					))}
				</ul>
			</section>

			<section>
				<h3>Steps</h3>
				<ol className="steps">
					{challenge.steps.map((step) => (
						<li key={step}>
							<Inline text={step} />
						</li>
					))}
				</ol>
			</section>

			<section>
				<h3>The trial</h3>
				<p className="idea">
					<Inline text={challenge.trial} />
				</p>
			</section>

			<section>
				<h3>Code you might need</h3>
				<dl className="code-refs">
					{challenge.code.map((ref) => (
						<div key={ref.code}>
							<dt>
								<pre>
									<code>{ref.code}</code>
								</pre>
							</dt>
							<dd>
								<Inline text={ref.text} />
							</dd>
						</div>
					))}
				</dl>
			</section>

			<section>
				<h3>
					Hints{" "}
					<span className="muted">
						({hintsShown} of {challenge.hints.length})
					</span>
				</h3>
				{hintsShown > 0 && (
					<ol className="hints">
						{challenge.hints.slice(0, hintsShown).map((hint) => (
							<li key={hint}>
								<Inline text={hint} />
							</li>
						))}
					</ol>
				)}
				{hintsShown < challenge.hints.length ? (
					<button type="button" className="button" onClick={() => setHintsShown(hintsShown + 1)}>
						{hintsShown === 0 ? "Show a hint" : "Show another hint"}
					</button>
				) : (
					<button type="button" className="button" onClick={() => setHintsShown(0)}>
						Hide hints
					</button>
				)}
			</section>

			<section>
				<h3>Learn more</h3>
				<p className="muted">Links can't open from inside the dashboard. Copy one into a new browser tab.</p>
				<ul className="docs">
					{challenge.docs.map((doc) => (
						<li key={doc.url}>
							<span>{doc.title}</span>
							<span className="doc-url">{doc.url}</span>
						</li>
					))}
				</ul>
			</section>
		</article>
	);
}
