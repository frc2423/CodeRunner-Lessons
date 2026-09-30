import type { ReactNode } from "react";

/**
 * Renders the little bit of markup the challenge text uses: `code`,
 * **bold** and *italic*.
 */
export function Inline({ text }: { text: string }) {
	const parts: ReactNode[] = [];
	const pattern = /`([^`]+)`|\*\*([^*]+)\*\*|\*([^*\s][^*]*)\*/g;
	let last = 0;
	for (const match of text.matchAll(pattern)) {
		const index = match.index ?? 0;
		if (index > last) parts.push(text.slice(last, index));
		if (match[1] !== undefined) parts.push(<code key={index}>{match[1]}</code>);
		else if (match[2] !== undefined) parts.push(<strong key={index}>{match[2]}</strong>);
		else parts.push(<em key={index}>{match[3]}</em>);
		last = index + match[0].length;
	}
	if (last < text.length) parts.push(text.slice(last));
	return <>{parts}</>;
}
