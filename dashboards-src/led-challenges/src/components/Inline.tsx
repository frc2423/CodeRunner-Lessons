import type { ReactNode } from "react";

/**
 * Renders the little bit of markup the challenge text uses: `code` and
 * **bold**.
 */
export function Inline({ text }: { text: string }) {
	const parts: ReactNode[] = [];
	const pattern = /`([^`]+)`|\*\*([^*]+)\*\*/g;
	let last = 0;
	for (const match of text.matchAll(pattern)) {
		const index = match.index ?? 0;
		if (index > last) parts.push(text.slice(last, index));
		parts.push(
			match[1] !== undefined ? (
				<code key={index}>{match[1]}</code>
			) : (
				<strong key={index}>{match[2]}</strong>
			),
		);
		last = index + match[0].length;
	}
	if (last < text.length) parts.push(text.slice(last));
	return <>{parts}</>;
}
