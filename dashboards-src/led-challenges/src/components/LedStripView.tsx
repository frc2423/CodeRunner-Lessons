import type { ReactNode } from "react";
import { cssColor, unpack } from "../leds";

/**
 * A row of LEDs, drawn on a dark strip like the real thing. Hover an LED to see
 * its index and color. `colors` are packed 0xRRGGBB numbers.
 */
export function LedStripView({
	colors,
	count,
	overlay,
}: {
	colors: readonly number[] | undefined;
	/** How many LEDs to draw while there are no colors yet. */
	count: number;
	/** Shown over the strip, e.g. while the robot isn't running. */
	overlay?: ReactNode;
}) {
	const length = colors?.length ?? count;
	return (
		<div className="strip-wrap">
			<div
				className={overlay ? "strip strip-idle" : "strip"}
				style={{ gridTemplateColumns: `repeat(${length}, minmax(0, 1fr))` }}
			>
				{Array.from({ length }, (_, i) => {
					const color = colors?.[i] ?? 0;
					const [r, g, b] = unpack(color);
					const lit = color !== 0;
					const css = cssColor(color);
					return (
						<span
							// LEDs never move, so the index is a stable key.
							key={i}
							className={lit ? "led led-on" : "led"}
							style={
								lit
									? {
											background: css,
											boxShadow: `0 0 ${4 + (Math.max(r, g, b) / 255) * 8}px ${css}`,
										}
									: undefined
							}
							title={`LED ${i}: ${r}, ${g}, ${b}`}
						/>
					);
				})}
				{overlay && <div className="strip-overlay">{overlay}</div>}
			</div>
			<div
				className="strip-ticks"
				style={{ gridTemplateColumns: `repeat(${length}, minmax(0, 1fr))` }}
				aria-hidden="true"
			>
				{Array.from({ length }, (_, i) => (
					<span key={i}>
						{i % 5 === 0 || i === length - 1 ? i : ""}
					</span>
				))}
			</div>
		</div>
	);
}
