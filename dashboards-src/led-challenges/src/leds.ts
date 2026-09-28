/**
 * A JavaScript copy of the lesson's `LedStrip.java`, used to animate each
 * challenge's goal (and, on the dev server, to fake the robot).
 *
 * Colors are packed as 0xRRGGBB numbers, the same format the robot logs to
 * `LEDs/Colors`.
 */

/** How many LEDs the lesson's strip has (`LedSubsystem.LED_COUNT`). */
export const DEFAULT_LED_COUNT = 30;

export type RGB = [number, number, number];

export function pack(r: number, g: number, b: number): number {
	return ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
}

export function unpack(color: number): RGB {
	return [(color >> 16) & 255, (color >> 8) & 255, color & 255];
}

export function cssColor(color: number): string {
	return `#${(color & 0xffffff).toString(16).padStart(6, "0")}`;
}

/** The same hue (degrees) / saturation / value (0–255) math as `LedStrip.setHSV`. */
export function hsvToRgb(hue: number, saturation: number, value: number): RGB {
	const h = (((hue % 360) + 360) % 360) / 60;
	const v = value / 255;
	const chroma = v * (saturation / 255);
	const x = chroma * (1 - Math.abs((h % 2) - 1));
	const [r, g, b] = [
		[chroma, x, 0],
		[x, chroma, 0],
		[0, chroma, x],
		[0, x, chroma],
		[x, 0, chroma],
		[chroma, 0, x],
	][Math.min(Math.floor(h), 5)];
	const m = v - chroma;
	return [
		Math.round((r + m) * 255),
		Math.round((g + m) * 255),
		Math.round((b + m) * 255),
	];
}

export class LedStrip {
	readonly colors: number[];

	constructor(length: number) {
		this.colors = new Array<number>(length).fill(0);
	}

	length(): number {
		return this.colors.length;
	}

	setRGB(index: number, r: number, g: number, b: number): void {
		this.colors[index] = pack(r, g, b);
	}

	setHSV(index: number, hue: number, saturation: number, value: number): void {
		this.setRGB(index, ...hsvToRgb(hue, saturation, value));
	}

	get(index: number): RGB {
		return unpack(this.colors[index]);
	}

	fill(r: number, g: number, b: number): void {
		for (let i = 0; i < this.colors.length; i++) this.setRGB(i, r, g, b);
	}

	clear(): void {
		this.colors.fill(0);
	}
}

/** Robot state a goal can react to, like `DriverStation` in Java. */
export type GoalRobotState = {
	enabled: boolean;
	autonomous: boolean;
	redAlliance: boolean;
};

/** The shape of a challenge's goal: the same two methods as `Led.java`. */
export type GoalProgram = {
	start?(leds: LedStrip): void;
	update(leds: LedStrip, seconds: number, robot: GoalRobotState): void;
};
