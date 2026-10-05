/** Where things are on the field, in meters. Matches the lesson's FieldConstants.java. */
export const FIELD = {
	/** We draw the blue half of the field. */
	length: 16.541 / 2,
	width: 8.069,
	hub: { x: 4.626, y: 4.035 },
	hubRadius: 0.6,
	hubScoringRadius: 0.75,
	shootingSpot: { x: 1.626, y: 4.035 },
	tower: { x: 1.2, y: 7.0 },
	towerZoneRadius: 0.8,
	humanPlayer: { x: 0.9, y: 0.9 },
	robotSize: 0.8,
	pieceRadius: 0.09,
};

/** "#rrggbb" from 0-255 channels. */
export function hex([r, g, b]: [number, number, number]): string {
	const part = (v: number) => Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, "0");
	return `#${part(r)}${part(g)}${part(b)}`;
}

/** Whether the LEDs are (nearly) off. */
export function ledOff([r, g, b]: [number, number, number]): boolean {
	return Math.max(r, g, b) < 30;
}
