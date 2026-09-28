import type { GoalProgram, LedStrip, RGB } from "./leds";

/**
 * Every challenge the dashboard offers. The `id`s must match the ones in the
 * lesson's `LedChallenges.java`, and each `className` must be a file in
 * `src/main/java/frc/robot/subsystems/LEDS/`.
 *
 * Text fields may use `backticks` for inline code.
 */

export type Level =
	| "Start here"
	| "Basics"
	| "Loops and decisions"
	| "Animation"
	| "Methods and arrays"
	| "Advanced"
	| "Real robots"
	| "Your turn";

export type CodeRef = { code: string; text: string };
export type DocLink = { title: string; url: string };
export type Check = { label: string; pass: (leds: RGB[]) => boolean };

export type Challenge = {
	id: string;
	number: number;
	title: string;
	className: string;
	level: Level;
	/** One or two sentences: what the finished challenge looks like. */
	summary: string;
	/** Programming ideas this challenge practises. */
	learn: string[];
	/** Step-by-step instructions. */
	steps: string[];
	/** Code the student will probably need. */
	code: CodeRef[];
	/** Revealed one at a time. */
	hints: string[];
	docs: DocLink[];
	/** Makes a fresh goal animation. Omitted when there is no single answer. */
	goal?: () => GoalProgram;
	/** Shown under the goal, e.g. when any color is fine. */
	goalNote?: string;
	/** Automatic checks for challenges whose result doesn't move. */
	checks?: Check[];
};

export const PACKAGE_DIR = "src/main/java/frc/robot/subsystems/LEDS";

export function challengeFile(challenge: Challenge): string {
	return `${PACKAGE_DIR}/${challenge.className}.java`;
}

// ---------------------------------------------------------------------------
// Helpers for checks

const TOLERANCE = 30;

function near(led: RGB | undefined, [r, g, b]: RGB, tolerance = TOLERANCE) {
	return (
		led !== undefined &&
		Math.abs(led[0] - r) <= tolerance &&
		Math.abs(led[1] - g) <= tolerance &&
		Math.abs(led[2] - b) <= tolerance
	);
}

const isOff = (led: RGB | undefined) => led !== undefined && near(led, [0, 0, 0], 0);
const isLit = (led: RGB | undefined) => led !== undefined && !isOff(led);
const same = (a: RGB, b: RGB) => near(a, b, 0);

const isOrange = (led: RGB | undefined) =>
	led !== undefined &&
	led[0] >= 200 &&
	led[1] >= 64 &&
	led[1] <= 190 &&
	led[2] <= 40;

const RED: RGB = [255, 0, 0];
const GREEN: RGB = [0, 255, 0];
const BLUE: RGB = [0, 0, 255];
const WHITE: RGB = [255, 255, 255];

const allOff = (leds: RGB[], except: number[]) =>
	leds.every((led, i) => except.includes(i) || isOff(led));

const uniform = (leds: RGB[]) =>
	leds.length > 0 && leds.every((led) => same(led, leds[0]));

const range = (start: number, end: number) =>
	Array.from({ length: Math.max(end - start, 0) }, (_, i) => start + i);

function fill(leds: LedStrip, [r, g, b]: RGB): void {
	leds.fill(r, g, b);
}

// ---------------------------------------------------------------------------
// Common code references

const SET_RGB: CodeRef = {
	code: "leds.setRGB(index, red, green, blue);",
	text: "Sets one LED. `index` is which LED (the first is 0). Each color amount goes from 0 (off) to 255 (full).",
};
const LENGTH: CodeRef = {
	code: "leds.length()",
	text: "How many LEDs the strip has (30). The last LED is `leds.length() - 1`.",
};
const SECONDS: CodeRef = {
	code: "seconds",
	text: "A parameter of `update`: how many seconds have passed since you clicked Run, like 0.0, 0.02, 0.04, …",
};
const CLEAR: CodeRef = { code: "leds.clear();", text: "Turns every LED off." };

const W3 = (page: string, title: string): DocLink => ({
	title: `W3Schools: ${title}`,
	url: `https://www.w3schools.com/java/${page}.asp`,
});
const ORACLE = (page: string, title: string): DocLink => ({
	title: `Java Tutorial: ${title}`,
	url: `https://docs.oracle.com/javase/tutorial/java/${page}.html`,
});

// ---------------------------------------------------------------------------

export const CHALLENGES: Challenge[] = [
	{
		id: "example",
		number: 0,
		title: "Example: Hello, LEDs",
		className: "Challenge00Example",
		level: "Start here",
		summary:
			"A finished example, so you can see how a challenge works. The first three LEDs turn red, green and blue.",
		learn: ["Running a challenge", "Method calls"],
		steps: [
			"Click **Start** in the Driver Station and wait for the robot program to start. The dot at the top of this dashboard turns green.",
			"Pick **Example: Hello, LEDs** above and click **Run**. The first three LEDs light up.",
			"Open the file. Every challenge is a class with an `update` method that the robot calls 50 times a second.",
			"Try it: change LED 2 to `leds.setRGB(2, 255, 255, 255);` (white). Save, click **Restart** in the Driver Station, and watch it change.",
		],
		code: [SET_RGB],
		hints: [
			"After you change Java code, the robot program has to be rebuilt: click **Restart** in the Driver Station. This dashboard runs your challenge again when the robot comes back.",
			"If your code has a mistake, the robot won't start. Look for red squiggles in the editor, or the error in the Driver Station's console.",
		],
		docs: [
			{
				title: "WPILib: Addressable LEDs",
				url: "https://docs.wpilib.org/en/stable/docs/software/hardware-apis/misc/addressable-leds.html",
			},
		],
		goal: () => ({
			update(leds) {
				leds.setRGB(0, 255, 0, 0);
				leds.setRGB(1, 0, 255, 0);
				leds.setRGB(2, 0, 0, 255);
			},
		}),
		checks: [
			{ label: "LED 0 is red", pass: (l) => near(l[0], RED) },
			{ label: "LED 1 is green", pass: (l) => near(l[1], GREEN) },
			{ label: "LED 2 is blue", pass: (l) => near(l[2], BLUE) },
		],
	},
	{
		id: "first-light",
		number: 1,
		title: "First Light",
		className: "Challenge01FirstLight",
		level: "Basics",
		summary: "Make the first LED red and the last LED blue.",
		learn: ["Statements", "Calling a method", "Counting from 0"],
		steps: [
			"Make the **first** LED red with `leds.setRGB(…)`.",
			"Make the **last** LED blue. The strip has 30 LEDs. What is the last one's index?",
			"Save, click **Restart** in the Driver Station, then **Run**.",
		],
		code: [SET_RGB],
		hints: [
			"Java counts from 0, so the first LED is LED 0.",
			"If the first LED is number 0, the 30th LED is number 29.",
			"Blue means no red, no green, and full blue: `0, 0, 255`.",
		],
		docs: [W3("java_methods", "Methods"), W3("java_syntax", "Java Syntax")],
		goal: () => ({
			update(leds) {
				leds.setRGB(0, 255, 0, 0);
				leds.setRGB(leds.length() - 1, 0, 0, 255);
			},
		}),
		checks: [
			{ label: "LED 0 is red", pass: (l) => near(l[0], RED) },
			{
				label: "The last LED is blue",
				pass: (l) => near(l[l.length - 1], BLUE),
			},
			{
				label: "Every other LED is off",
				pass: (l) => allOff(l, [0, l.length - 1]),
			},
		],
	},
	{
		id: "mixing-colors",
		number: 2,
		title: "Mixing Colors",
		className: "Challenge02MixingColors",
		level: "Basics",
		summary:
			"Every LED holds a tiny red, green and blue light. Mix them to make yellow, cyan, magenta, white, orange and a dim red.",
		learn: ["RGB colors", "Integer values", "Arguments"],
		steps: [
			"LED 0: **yellow**.",
			"LED 1: **cyan** (light blue).",
			"LED 2: **magenta** (pink-purple).",
			"LED 3: **white**.",
			"LED 4: **orange**.",
			"LED 5: **dim red**, about a quarter as bright as full red.",
		],
		code: [SET_RGB],
		hints: [
			"Light mixes differently from paint: red light + green light = yellow light.",
			"Cyan is green + blue. Magenta is red + blue. White is all three at full.",
			"Orange is full red with about half green.",
			"Brightness is how big the numbers are. A quarter of 255 is about 64.",
		],
		docs: [
			{
				title: "Wikipedia: RGB color model",
				url: "https://en.wikipedia.org/wiki/RGB_color_model",
			},
		],
		goal: () => ({
			update(leds) {
				leds.setRGB(0, 255, 255, 0);
				leds.setRGB(1, 0, 255, 255);
				leds.setRGB(2, 255, 0, 255);
				leds.setRGB(3, 255, 255, 255);
				leds.setRGB(4, 255, 128, 0);
				leds.setRGB(5, 64, 0, 0);
			},
		}),
		checks: [
			{ label: "LED 0 is yellow", pass: (l) => near(l[0], [255, 255, 0]) },
			{ label: "LED 1 is cyan", pass: (l) => near(l[1], [0, 255, 255]) },
			{ label: "LED 2 is magenta", pass: (l) => near(l[2], [255, 0, 255]) },
			{ label: "LED 3 is white", pass: (l) => near(l[3], WHITE) },
			{ label: "LED 4 is orange", pass: (l) => isOrange(l[4]) },
			{
				label: "LED 5 is dim red",
				pass: (l) =>
					l[5] !== undefined &&
					l[5][0] >= 30 &&
					l[5][0] <= 100 &&
					l[5][1] <= 10 &&
					l[5][2] <= 10,
			},
		],
	},
	{
		id: "variables",
		number: 3,
		title: "Variables",
		className: "Challenge03Variables",
		level: "Basics",
		summary:
			"Store your favorite color in variables and use them to light LEDs 0 to 4. Then use a `position` variable to place three white LEDs.",
		learn: ["Declaring variables", "int", "Expressions like position + 1"],
		steps: [
			"Make three `int` variables, `red`, `green` and `blue`, holding your favorite color.",
			"Light LEDs 0, 1, 2, 3 and 4 using the **variables**, not numbers: `leds.setRGB(0, red, green, blue);`",
			"Make an `int` variable called `position` set to `10`. Light the LEDs at `position`, `position + 1` and `position + 2` in white.",
			"Change `position` to `20` and your color variables to something else. Restart and run it again: everything moves and changes together.",
		],
		code: [
			{
				code: "int red = 200;",
				text: "Declares a variable: its type (`int`, a whole number), its name, and its starting value.",
			},
			SET_RGB,
		],
		hints: [
			"A variable is a named box that holds a value. You write its name wherever you want its value.",
			"Declare variables before the line that uses them, inside `update`.",
			"`leds.setRGB(position + 1, 255, 255, 255);` lights the LED one after `position`.",
		],
		docs: [
			W3("java_variables", "Variables"),
			ORACLE("nutsandbolts/variables", "Variables"),
		],
		goal: () => ({
			update(leds) {
				for (let i = 0; i < 5; i++) leds.setRGB(i, 0, 180, 160);
				for (let i = 10; i < 13; i++) leds.setRGB(i, 255, 255, 255);
			},
		}),
		goalNote: "Your favorite color can be anything.",
		checks: [
			{
				label: "LEDs 0 to 4 are lit",
				pass: (l) => range(0, 5).every((i) => isLit(l[i])),
			},
			{
				label: "LEDs 0 to 4 are all the same color",
				pass: (l) => uniform(l.slice(0, 5)),
			},
			{
				label: "LEDs 10, 11 and 12 are white",
				pass: (l) => [10, 11, 12].every((i) => near(l[i], WHITE)),
			},
			{
				label: "Every other LED is off",
				pass: (l) => allOff(l, [0, 1, 2, 3, 4, 10, 11, 12]),
			},
		],
	},
	{
		id: "fill-strip",
		number: 4,
		title: "Fill the Strip",
		className: "Challenge04FillStrip",
		level: "Loops and decisions",
		summary:
			"Light every LED in the same color, with a `for` loop instead of 30 lines of code.",
		learn: ["for loops", "Loop variables", "leds.length()"],
		steps: [
			"Write a `for` loop whose variable `i` goes 0, 1, 2, … up to the last LED.",
			"Inside the loop, set LED `i` to your color.",
			"Use `leds.length()` in the loop instead of the number 30. Then your code works on a strip of any size.",
		],
		code: [
			{
				code: "for (int i = 0; i < 5; i++) {\n  // runs 5 times: i is 0, 1, 2, 3, 4\n}",
				text: "A `for` loop: start value; keep going while this is true; what to do after each time round.",
			},
			LENGTH,
			SET_RGB,
		],
		hints: [
			"The code inside the loop's `{ }` runs once for each value of `i`.",
			"Use `i` as the LED index: `leds.setRGB(i, …)`.",
			"`i < leds.length()` stops before 30, so the last `i` is 29, the last LED. `i <= leds.length()` would go one too far.",
		],
		docs: [W3("java_for_loop", "For Loop"), ORACLE("nutsandbolts/for", "The for Statement")],
		goal: () => ({
			update(leds) {
				fill(leds, [0, 255, 0]);
			},
		}),
		goalNote: "Any color works.",
		checks: [
			{ label: "Every LED is lit", pass: (l) => l.every(isLit) },
			{ label: "Every LED is the same color", pass: uniform },
		],
	},
	{
		id: "stripes",
		number: 5,
		title: "Stripes",
		className: "Challenge05Stripes",
		level: "Loops and decisions",
		summary:
			"Alternate the colors: even-numbered LEDs orange, odd-numbered LEDs blue.",
		learn: ["if / else", "The % (remainder) operator", "== comparison"],
		steps: [
			"Loop over every LED, like in Fill the Strip.",
			"Inside the loop, use `if` to check whether `i` is even.",
			"Even: orange. Otherwise (`else`): blue.",
		],
		code: [
			{
				code: "if (condition) {\n  // runs when condition is true\n} else {\n  // runs when it is false\n}",
				text: "Chooses which code runs.",
			},
			{
				code: "i % 2",
				text: "The remainder after dividing `i` by 2: `0` for even numbers, `1` for odd numbers.",
			},
			{ code: "a == b", text: "`true` when `a` equals `b`. (One `=` stores a value; two compare.)" },
			SET_RGB,
		],
		hints: [
			"`7 % 2` is 1 (7 ÷ 2 = 3 remainder 1). `8 % 2` is 0.",
			"`if (i % 2 == 0)` is true for 0, 2, 4, …",
			"Bonus: make stripes 3 LEDs wide. Try `(i / 3) % 2`: integer division throws away the remainder.",
		],
		docs: [
			W3("java_conditions", "If … Else"),
			W3("java_operators", "Operators (including %)"),
		],
		goal: () => ({
			update(leds) {
				for (let i = 0; i < leds.length(); i++) {
					if (i % 2 === 0) leds.setRGB(i, 255, 128, 0);
					else leds.setRGB(i, 0, 0, 255);
				}
			},
		}),
		checks: [
			{
				label: "Even LEDs are orange",
				pass: (l) => l.every((led, i) => i % 2 === 1 || isOrange(led)),
			},
			{
				label: "Odd LEDs are blue",
				pass: (l) => l.every((led, i) => i % 2 === 0 || near(led, BLUE)),
			},
		],
	},
	{
		id: "gradient",
		number: 6,
		title: "Gradient",
		className: "Challenge06Gradient",
		level: "Loops and decisions",
		summary:
			"Fade smoothly from red at the first LED to blue at the last, working out each LED's color with math.",
		learn: ["Arithmetic", "double vs int", "Casting", "Integer division"],
		steps: [
			"Loop over every LED.",
			"Work out how far along the strip LED `i` is, as a `double` from `0.0` (first LED) to `1.0` (last LED).",
			"Blue is `255 * fraction`. Red is `255 * (1 - fraction)`. Green stays 0.",
			"`setRGB` needs whole numbers (`int`), so convert your `double`s before you use them.",
		],
		code: [
			{
				code: "double fraction = (double) i / (leds.length() - 1);",
				text: "How far along LED `i` is. The `(double)` matters: see the first hint.",
			},
			{
				code: "(int) Math.round(x)",
				text: "Rounds the `double` `x` to the nearest whole number and makes it an `int`.",
			},
			SET_RGB,
		],
		hints: [
			"In Java, `int / int` throws away the remainder: `14 / 29` is `0`, not `0.48`. That's why `i` is turned into a `double` first.",
			"The last LED is `leds.length() - 1`. Dividing by that makes the last LED's fraction exactly `1.0`.",
			"`int red = (int) Math.round(255 * (1 - fraction));`",
		],
		docs: [
			W3("java_type_casting", "Type Casting"),
			W3("java_data_types", "Data Types"),
			W3("java_math", "Math"),
		],
		goal: () => ({
			update(leds) {
				const last = leds.length() - 1;
				for (let i = 0; i <= last; i++) {
					const t = i / last;
					leds.setRGB(i, Math.round(255 * (1 - t)), 0, Math.round(255 * t));
				}
			},
		}),
		checks: [
			{ label: "The first LED is red", pass: (l) => near(l[0], RED, 10) },
			{ label: "The last LED is blue", pass: (l) => near(l[l.length - 1], BLUE, 10) },
			{
				label: "Red fades out along the strip",
				pass: (l) =>
					l.every((led, i) => i === 0 || led[0] <= l[i - 1][0]) &&
					l[0][0] > l[l.length - 1][0],
			},
			{
				label: "Blue fades in along the strip",
				pass: (l) =>
					l.every((led, i) => i === 0 || led[2] >= l[i - 1][2]) &&
					l[0][2] < l[l.length - 1][2],
			},
			{
				label: "The middle LED is a purple mix",
				pass: (l) => {
					const mid = l[Math.floor(l.length / 2)];
					return mid !== undefined && mid[0] >= 90 && mid[2] >= 90 && mid[1] <= 10;
				},
			},
		],
	},
	{
		id: "blink",
		number: 7,
		title: "Blink",
		className: "Challenge07Blink",
		level: "Animation",
		summary:
			"Blink the whole strip yellow: on for half a second, off for half a second, forever.",
		learn: ["Using time", "boolean", "% with decimals"],
		steps: [
			"`update` gets `seconds`, the time since you clicked Run.",
			"Work out a `boolean` that is `true` during the first half of every second and `false` during the second half.",
			"If it is `true`, fill the strip with yellow. Otherwise turn every LED off.",
		],
		code: [
			SECONDS,
			{
				code: "seconds % 1.0",
				text: "The part of `seconds` after the decimal point: it counts 0.0 → 0.99 and starts again every second.",
			},
			{
				code: "boolean on = …;",
				text: "A `boolean` holds `true` or `false`, for example the result of a comparison like `x < 0.5`.",
			},
			CLEAR,
		],
		hints: [
			"`2.3 % 1.0` is `0.3`. `7.8 % 1.0` is `0.8`.",
			"`boolean on = seconds % 1.0 < 0.5;`",
			"Don't forget to turn the LEDs off: they keep their color until you change them.",
		],
		docs: [W3("java_booleans", "Booleans"), W3("java_operators", "Operators")],
		goal: () => ({
			update(leds, seconds) {
				if (seconds % 1 < 0.5) fill(leds, [255, 255, 0]);
				else leds.clear();
			},
		}),
	},
	{
		id: "moving-dot",
		number: 8,
		title: "Moving Dot",
		className: "Challenge08MovingDot",
		level: "Animation",
		summary:
			"A green dot travels along the strip and wraps back to LED 0 at the end.",
		learn: ["Fields (object state)", "Counters", "Wrapping around"],
		steps: [
			"Add a **field** to the class, above `update`: `private int position = 0;`",
			"In `update`: turn every LED off, light LED `position` green, then add 1 to `position`.",
			"When `position` goes past the last LED, set it back to 0. Run it: the dot is fast!",
			"Slow it down: add a second field that counts updates, and only move the dot every 5th update.",
		],
		code: [
			{
				code: "private int position = 0;",
				text: "A field. Unlike a variable inside `update`, it keeps its value between updates.",
			},
			{ code: "position++;", text: "Adds 1 to `position`." },
			CLEAR,
			SET_RGB,
			LENGTH,
		],
		hints: [
			"A variable declared inside `update` is created fresh every update, so it can't remember anything.",
			"`if (position >= leds.length()) { position = 0; }`",
			"Moving every 5th update: count with `updates++`, and move when `updates % 5 == 0`.",
		],
		docs: [
			ORACLE("javaOO/variables", "Fields (member variables)"),
			W3("java_class_attributes", "Class Attributes"),
		],
		goal: () => {
			let position = 0;
			let updates = 0;
			return {
				update(leds) {
					leds.clear();
					leds.setRGB(position, 0, 255, 0);
					updates++;
					if (updates % 5 === 0) position = (position + 1) % leds.length();
				},
			};
		},
	},
	{
		id: "progress-bar",
		number: 9,
		title: "Progress Bar",
		className: "Challenge09ProgressBar",
		level: "Animation",
		summary:
			"A green bar grows from empty to full over 3 seconds, then starts again. Unlit LEDs glow dim gray.",
		learn: ["Combining loops and if", "Proportions", "Repeating with %"],
		steps: [
			"Work out how far through the current 3-second cycle we are, as a fraction from 0.0 to 1.0.",
			"Turn it into a number of LEDs: `int lit = (int) (fraction * leds.length());`",
			"Loop over every LED: green if `i < lit`, dim gray (`20, 20, 20`) if not.",
		],
		code: [
			SECONDS,
			{
				code: "seconds % 3.0",
				text: "Counts from 0.0 up to 3.0, then starts again.",
			},
			{
				code: "(int) x",
				text: "Turns a `double` into an `int` by chopping off the decimals.",
			},
			SET_RGB,
		],
		hints: [
			"`double fraction = (seconds % 3.0) / 3.0;`",
			"LED `i` is part of the bar when `i < lit`.",
			"Bonus: make the bar's color go from red to yellow to green as it fills.",
		],
		docs: [W3("java_type_casting", "Type Casting"), W3("java_conditions", "If … Else")],
		goal: () => ({
			update(leds, seconds) {
				const lit = Math.floor(((seconds % 3) / 3) * leds.length());
				for (let i = 0; i < leds.length(); i++) {
					if (i < lit) leds.setRGB(i, 0, 255, 0);
					else leds.setRGB(i, 20, 20, 20);
				}
			},
		}),
	},
	{
		id: "flag",
		number: 10,
		title: "Flag",
		className: "Challenge10Flag",
		level: "Methods and arrays",
		summary:
			"Paint three equal sections, red, white and blue, by writing one method and calling it three times.",
		learn: ["Writing methods", "Parameters", "Reusing code"],
		steps: [
			"Finish the `fillRange` method at the bottom of the file: a loop from `start` up to (not including) `end` that sets each LED to the given color.",
			"In `update`, work out the size of one section: a third of the strip.",
			"Call `fillRange` three times: LEDs `0` to `size` red, `size` to `2 * size` white, and `2 * size` to the end blue.",
		],
		code: [
			{
				code: "fillRange(leds, 0, size, 255, 0, 0);",
				text: "Calls your method. The values are copied into its parameters, in order.",
			},
			{
				code: "private void fillRange(LedStrip leds, int start, int end, int red, int green, int blue)",
				text: "The method you are finishing. `void` means it doesn't give back a value.",
			},
			LENGTH,
		],
		hints: [
			"Inside `fillRange`, the loop is `for (int i = start; i < end; i++)`.",
			"`int size = leds.length() / 3;` is 10 for a 30-LED strip.",
			"Use `leds.length()` as the last section's end, so the whole strip is covered even if the length doesn't divide by 3.",
			"Bonus: pick a different flag, or a flag with four stripes. How much of your code changes?",
		],
		docs: [W3("java_methods", "Methods"), W3("java_methods_param", "Method Parameters")],
		goal: () => ({
			update(leds) {
				const size = Math.floor(leds.length() / 3);
				for (let i = 0; i < leds.length(); i++) {
					if (i < size) leds.setRGB(i, 255, 0, 0);
					else if (i < 2 * size) leds.setRGB(i, 255, 255, 255);
					else leds.setRGB(i, 0, 0, 255);
				}
			},
		}),
		checks: [
			{
				label: "The first third is red",
				pass: (l) => l.slice(0, l.length / 3).every((led) => near(led, RED)),
			},
			{
				label: "The middle third is white",
				pass: (l) =>
					l.slice(l.length / 3, (2 * l.length) / 3).every((led) => near(led, WHITE)),
			},
			{
				label: "The last third is blue",
				pass: (l) => l.slice((2 * l.length) / 3).every((led) => near(led, BLUE)),
			},
		],
	},
	{
		id: "rainbow",
		number: 11,
		title: "Rainbow",
		className: "Challenge11Rainbow",
		level: "Methods and arrays",
		summary:
			"Spread a whole rainbow across the strip, then make it slide along.",
		learn: ["HSV colors", "Math with the loop variable", "Animating with time"],
		steps: [
			"Loop over every LED. Give LED `i` a hue that goes from 0 at the first LED almost all the way round to 360 at the last.",
			"Use `leds.setHSV(i, hue, 255, 255)` for full, bright colors.",
			"Make it move: add `(int) (seconds * 90)` to every hue. The rainbow shifts 90 degrees each second.",
		],
		code: [
			{
				code: "leds.setHSV(index, hue, saturation, value);",
				text: "`hue` is the angle round the color wheel: 0 red, 60 yellow, 120 green, 180 cyan, 240 blue, 300 magenta. It wraps, so 360 is red again. `saturation` and `value` go from 0 to 255.",
			},
			LENGTH,
			SECONDS,
		],
		hints: [
			"`int hue = i * 360 / leds.length();` Multiply first: `i / leds.length()` on its own would be 0 (integer division).",
			"Hues above 360 are fine: `setHSV` wraps them around the color wheel.",
			"Try a faster or slower speed than 90, or a negative one.",
		],
		docs: [
			{
				title: "Wikipedia: HSL and HSV",
				url: "https://en.wikipedia.org/wiki/HSL_and_HSV",
			},
			W3("java_operators", "Operators"),
		],
		goal: () => ({
			update(leds, seconds) {
				const shift = Math.floor(seconds * 90);
				for (let i = 0; i < leds.length(); i++) {
					leds.setHSV(i, Math.floor((i * 360) / leds.length()) + shift, 255, 255);
				}
			},
		}),
	},
	{
		id: "palette",
		number: 12,
		title: "Color Palette",
		className: "Challenge12Palette",
		level: "Methods and arrays",
		summary:
			"Store 4 colors in an array, repeat them along the strip, and shift the pattern every quarter of a second.",
		learn: ["Arrays", "Array length", "Indexing with %"],
		steps: [
			"Finish the `palette` array at the top of the class: 4 colors in total.",
			"Loop over every LED. LED `i` gets `palette[i % palette.length]`: color 0, 1, 2, 3, 0, 1, 2, 3, …",
			"Make it move: every 0.25 seconds, shift the pattern one LED. Work out a `shift` from `seconds` and add it to `i` before the `%`.",
		],
		code: [
			{
				code: "Color[] palette = { Color.kRed, Color.kOrange };",
				text: "An array: several values of one type, in order. `palette[0]` is the first, `palette.length` is how many.",
			},
			{
				code: "leds.setColor(index, color);",
				text: "Sets one LED to a `Color`, such as `Color.kPurple` or `Color.kGold`.",
			},
			{ code: "int shift = (int) (seconds / 0.25);", text: "Goes up by 1 every quarter of a second." },
		],
		hints: [
			"`palette[4]` doesn't exist in a 4-color array (it would crash). `i % palette.length` always gives 0 to 3.",
			"`leds.setColor(i, palette[(i + shift) % palette.length]);`",
			"Type `Color.k` in the editor to see every named color.",
		],
		docs: [
			W3("java_arrays", "Arrays"),
			ORACLE("nutsandbolts/arrays", "Arrays"),
			{
				title: "WPILib: Color class",
				url: "https://github.wpilib.org/allwpilib/docs/release/java/edu/wpi/first/wpilibj/util/Color.html",
			},
		],
		goal: () => {
			const palette: RGB[] = [
				[255, 0, 0],
				[255, 165, 0],
				[255, 255, 0],
				[255, 255, 255],
			];
			return {
				update(leds, seconds) {
					const shift = Math.floor(seconds / 0.25);
					for (let i = 0; i < leds.length(); i++) {
						leds.setRGB(i, ...palette[(i + shift) % palette.length]);
					}
				},
			};
		},
		goalNote: "Your palette can be any 4 colors, and it can move either way.",
	},
	{
		id: "scanner",
		number: 13,
		title: "Scanner",
		className: "Challenge13Scanner",
		level: "Advanced",
		summary:
			"A red dot bounces back and forth between the ends of the strip, like a robot's scanning eye.",
		learn: ["Several fields working together", "Direction", "Checking boundaries"],
		steps: [
			"Add fields: `position`, `direction` (`1` for forwards, `-1` for backwards) and an update counter.",
			"Each update: turn the LEDs off and draw the dot.",
			"Every 2 updates, move: `position += direction;`",
			"When the dot reaches the first or last LED, turn it around: `direction = -direction;`",
			"Bonus: leave a fading tail. Instead of turning the LEDs off, make each one dimmer (multiply its red by 0.7).",
		],
		code: [
			{ code: "position += direction;", text: "Adds `direction` to `position`: forwards when it is 1, backwards when it is -1." },
			{ code: "direction = -direction;", text: "Flips 1 to -1, or -1 to 1." },
			{ code: "leds.getRed(i)", text: "How much red LED `i` has now (0 to 255). Also `getGreen` and `getBlue`." },
			CLEAR,
		],
		hints: [
			"Turn around when `position == 0 || position == leds.length() - 1`. `||` means \"or\".",
			"Check the ends right after you move, so the dot never steps off the strip.",
			"Tail: `leds.setRGB(i, (int) (leds.getRed(i) * 0.7), 0, 0);` for every LED, before drawing the dot.",
		],
		docs: [ORACLE("javaOO/variables", "Fields"), W3("java_operators", "Operators (logical ||)")],
		goal: () => {
			let position = 0;
			let direction = 1;
			let updates = 0;
			return {
				update(leds) {
					for (let i = 0; i < leds.length(); i++) {
						leds.setRGB(i, Math.floor(leds.get(i)[0] * 0.7), 0, 0);
					}
					leds.setRGB(position, 255, 0, 0);
					updates++;
					if (updates % 2 === 0) {
						position += direction;
						if (position === 0 || position === leds.length() - 1) direction = -direction;
					}
				},
			};
		},
		goalNote: "The goal includes the bonus tail.",
	},
	{
		id: "twinkle",
		number: 14,
		title: "Twinkle",
		className: "Challenge14Twinkle",
		level: "Advanced",
		summary:
			"LEDs sparkle bright white at random, then slowly fade out, like stars.",
		learn: ["Random numbers", "Probability", "Reading and changing state"],
		steps: [
			"Loop over every LED.",
			"Read its current color with `getRed`, `getGreen` and `getBlue`, and set it to 90% of that, so it fades.",
			"Roll a random number. With a 2% chance, make the LED bright white instead.",
		],
		code: [
			{
				code: "random.nextDouble()",
				text: "A random `double` from 0.0 up to (not including) 1.0. `random.nextDouble() < 0.02` is true 2% of the time.",
			},
			{ code: "leds.getRed(i)", text: "How much red LED `i` has now (0 to 255). Also `getGreen` and `getBlue`." },
			{ code: "(int) (x * 0.9)", text: "90% of `x`, as a whole number." },
			SET_RGB,
		],
		hints: [
			"The `random` field is already made for you at the top of the class.",
			"An LED that fades by 10% each update is almost dark after about 40 updates (under a second).",
			"Bonus: sparkle in random colors with `random.nextInt(360)` as a hue for `setHSV`.",
		],
		docs: [
			{
				title: "Java API: Random",
				url: "https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Random.html",
			},
			W3("java_math", "Math"),
		],
		goal: () => ({
			update(leds) {
				for (let i = 0; i < leds.length(); i++) {
					if (Math.random() < 0.02) leds.setRGB(i, 255, 255, 255);
					else {
						const [r, g, b] = leds.get(i);
						leds.setRGB(i, Math.floor(r * 0.9), Math.floor(g * 0.9), Math.floor(b * 0.9));
					}
				}
			},
		}),
	},
	{
		id: "morse-code",
		number: 15,
		title: "Morse Code",
		className: "Challenge15MorseCode",
		level: "Advanced",
		summary:
			"Blink a message (SOS) in Morse code with the whole strip, then repeat it.",
		learn: ["Strings and chars", "Building a String", "Lookup tables", "Planning in start()"],
		steps: [
			"Morse timing uses **units** of 0.2 seconds. Dot: on 1 unit. Dash: on 3 units. Between dots and dashes: off 1 unit. Between letters: off 3 units. Between words, and before repeating: off 7 units.",
			"In `start`, build a pattern String with one character per unit: `'1'` for on, `'0'` for off. \"E\" (a single dot) plus the pause before repeating is `\"10000000\"`.",
			"Loop over the letters of `MESSAGE`. For each letter, look up its code in `MORSE`, then loop over the dots and dashes to add to the pattern.",
			"In `update`, work out which unit it is, `(int) (seconds / UNIT)`, wrap it with `%` to the pattern's length, and turn the strip on or off.",
		],
		code: [
			{ code: "MESSAGE.charAt(i)", text: "The `char` at position `i` of a String (the first is 0)." },
			{ code: "MORSE[c - 'A']", text: "The Morse code for the letter `c`. Characters are numbers underneath, so `'C' - 'A'` is 2." },
			{
				code: "StringBuilder pattern = new StringBuilder();\npattern.append(\"1\");\nString done = pattern.toString();",
				text: "Builds a String piece by piece.",
			},
			{ code: "pattern.charAt(unit) == '1'", text: "Is the strip on during this unit?" },
		],
		hints: [
			"For each dot or dash, add its on-time (`\"1\"` or `\"111\"`) and then one off unit (`\"0\"`).",
			"After each letter you have already added 1 off unit, so add 2 more to make 3.",
			"SOS should come out as `1010100011101110111000101010000000`.",
			"Bonus: change `MESSAGE` to your team name. Handle spaces between words.",
		],
		docs: [
			W3("java_strings", "Strings"),
			{
				title: "Java API: StringBuilder",
				url: "https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/StringBuilder.html",
			},
			{ title: "Wikipedia: Morse code", url: "https://en.wikipedia.org/wiki/Morse_code" },
		],
		goal: () => {
			const pattern = "1010100011101110111000101010000000";
			return {
				update(leds, seconds) {
					const on = pattern[Math.floor(seconds / 0.2) % pattern.length] === "1";
					if (on) fill(leds, [255, 200, 0]);
					else leds.clear();
				},
			};
		},
		goalNote: "Any color works.",
	},
	{
		id: "automaton",
		number: 16,
		title: "Cellular Automaton",
		className: "Challenge16Automaton",
		level: "Advanced",
		summary:
			"Each LED is a cell, alive or dead. From one live cell in the middle, a simple rule grows a surprising pattern (Rule 90).",
		learn: ["boolean arrays", "Neighbors and wrap-around", "Copying state safely"],
		steps: [
			"In `start`, make a `boolean` array with one cell per LED, and set only the middle cell to `true`.",
			"Every 0.15 seconds, make a **new** array for the next generation. Cell `i` is alive if exactly one of its neighbors (`i - 1` and `i + 1`) is alive now.",
			"The strip wraps around: the first cell's left neighbor is the last cell, and the last cell's right neighbor is the first.",
			"Replace the old array with the new one, then draw: alive cells lit, dead cells off.",
		],
		code: [
			{ code: "boolean[] cells = new boolean[leds.length()];", text: "An array of `false` values, one per LED." },
			{ code: "left != right", text: "For two `boolean`s: true when exactly one of them is true." },
			{ code: "(i + 1) % cells.length", text: "The next index, wrapping from the last back to 0." },
			{ code: "(i - 1 + cells.length) % cells.length", text: "The previous index, wrapping from 0 to the last." },
		],
		hints: [
			"Why a new array? If you changed `cells` while looping, later cells would see neighbors that already changed.",
			"Remember when you last stepped in a `double` field. Step again when `seconds - lastStep >= STEP`.",
			"Bonus: try Rule 30. A cell is alive when `left != (center || right)`.",
		],
		docs: [
			W3("java_arrays", "Arrays"),
			{ title: "Wikipedia: Rule 90", url: "https://en.wikipedia.org/wiki/Rule_90" },
		],
		goal: () => {
			let cells: boolean[] = [];
			let lastStep = 0;
			return {
				start(leds) {
					cells = new Array<boolean>(leds.length()).fill(false);
					cells[Math.floor(leds.length() / 2)] = true;
				},
				update(leds, seconds) {
					if (seconds - lastStep >= 0.15) {
						lastStep = seconds;
						const n = cells.length;
						cells = cells.map((_, i) => cells[(i - 1 + n) % n] !== cells[(i + 1) % n]);
					}
					for (let i = 0; i < cells.length; i++) {
						if (cells[i]) leds.setRGB(i, 0, 255, 120);
						else leds.setRGB(i, 0, 0, 0);
					}
				},
			};
		},
		goalNote: "Any color works.",
	},
	{
		id: "robot-status",
		number: 17,
		title: "Robot Status",
		className: "Challenge17RobotStatus",
		level: "Real robots",
		summary:
			"Show the robot's state like a real robot does: breathe in the alliance color when disabled, yellow in autonomous, green in teleop.",
		learn: ["Using a library class", "if / else if / else", "Math.sin", "Optional"],
		steps: [
			"Add `import edu.wpi.first.wpilibj.DriverStation;` at the top of the file.",
			"Enabled in autonomous: solid yellow. Enabled in teleop: solid green.",
			"Disabled: fill the strip with the alliance color (red or blue) and make its brightness rise and fall about once every 2 seconds.",
			"Test it: change **Enable/Disable**, the mode and the alliance in the Driver Station while the challenge runs.",
		],
		code: [
			{ code: "DriverStation.isEnabled()", text: "`true` while the robot is enabled." },
			{ code: "DriverStation.isAutonomous()", text: "`true` in autonomous mode." },
			{
				code: "DriverStation.getAlliance()",
				text: "An `Optional<Alliance>`: it may be empty (not known yet). Check `.isPresent()` before `.get()`.",
			},
			{
				code: "Math.sin(seconds * Math.PI)",
				text: "Swings smoothly between -1 and 1, once every 2 seconds.",
			},
		],
		hints: [
			"Start with `if (DriverStation.isEnabled() && DriverStation.isAutonomous()) { … } else if (DriverStation.isEnabled()) { … } else { … }`.",
			"Brightness from 0 to 255: `(int) ((Math.sin(seconds * Math.PI) + 1) / 2 * 255)`.",
			"Red alliance: `DriverStation.getAlliance().isPresent() && DriverStation.getAlliance().get() == DriverStation.Alliance.Red`.",
		],
		docs: [
			{
				title: "WPILib: DriverStation class",
				url: "https://github.wpilib.org/allwpilib/docs/release/java/edu/wpi/first/wpilibj/DriverStation.html",
			},
			W3("java_math", "Math"),
			W3("java_conditions_elseif", "else if"),
		],
		goal: () => ({
			update(leds, seconds, robot) {
				if (robot.enabled && robot.autonomous) fill(leds, [255, 255, 0]);
				else if (robot.enabled) fill(leds, [0, 255, 0]);
				else {
					const brightness = Math.round(((Math.sin(seconds * Math.PI) + 1) / 2) * 255);
					fill(leds, robot.redAlliance ? [brightness, 0, 0] : [0, 0, brightness]);
				}
			},
		}),
		goalNote: "The goal follows the Driver Station too.",
	},
	{
		id: "freestyle",
		number: 18,
		title: "Freestyle",
		className: "Challenge18Freestyle",
		level: "Your turn",
		summary:
			"Your own light show. Combine anything you've learned, or invent something new.",
		learn: ["Designing your own program"],
		steps: [
			"Plan it first: what should the strip look like, and how should it change over time?",
			"Some ideas: a countdown that shrinks and then flashes; two dots that move in opposite directions and change color when they cross; flickering fire made of random warm colors.",
			"Write it one small step at a time, and run it after each step.",
		],
		code: [
			SET_RGB,
			{ code: "leds.setHSV(index, hue, saturation, value);", text: "Sets a color by hue (0 to 360) instead of red, green and blue." },
			{ code: "leds.getRed(i)", text: "Reads an LED's current color, also `getGreen` and `getBlue`." },
			LENGTH,
			SECONDS,
			CLEAR,
		],
		hints: [
			"Look back at your other challenges: fields for things that move, `seconds` for timing, `Random` for surprises.",
			"Want to keep this show and start another? Write the new one in its own class that `implements Led`, then point the `\"freestyle\"` line in `LedChallenges.java` at it: `CHALLENGES.put(\"freestyle\", MyShow::new);`",
		],
		docs: [
			{
				title: "WPILib: Addressable LEDs",
				url: "https://docs.wpilib.org/en/stable/docs/software/hardware-apis/misc/addressable-leds.html",
			},
		],
	},
];

export function findChallenge(id: string | undefined): Challenge | undefined {
	return CHALLENGES.find((challenge) => challenge.id === id);
}
