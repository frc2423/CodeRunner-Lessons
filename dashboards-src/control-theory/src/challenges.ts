import type { MechanismKind } from "./lab";

/**
 * Every challenge the dashboard offers. The `id`s must match the ones in the
 * lesson's `Challenges.java`, and each `className` must be a file in
 * `src/main/java/frc/robot/challenges/`. The robot program owns what each
 * trial does and how it is graded; this file holds what the student reads.
 *
 * Text fields may use `backticks` for inline code and **stars** for bold.
 */

export type Level =
	| "Start here"
	| "Feedback basics"
	| "Position control"
	| "Motion profiles"
	| "Models and LQR"
	| "Noise and estimation"
	| "Capstone";

export type CodeRef = { code: string; text: string };
export type DocLink = { title: string; url: string };
/** A section of Controls Engineering in FRC. */
export type BookRef = { section: string; title: string };

export type Slider = {
	min: number;
	max: number;
	step: number;
	/** Spread the slider evenly over orders of magnitude. */
	log?: boolean;
	unit?: string;
};

export type Challenge = {
	id: string;
	number: number;
	title: string;
	className: string;
	level: Level;
	mechanism: MechanismKind;
	/** One or two sentences: what the trial asks for. */
	summary: string;
	/** Ideas this challenge practises. */
	learn: string[];
	/** The control theory behind it, a paragraph each. */
	idea: string[];
	steps: string[];
	code: CodeRef[];
	/** Revealed one at a time. */
	hints: string[];
	book: BookRef[];
	docs: DocLink[];
	/** Slider ranges for the challenge's tunable numbers, by name. */
	sliders?: Record<string, Slider>;
};

export const PACKAGE_DIR = "src/main/java/frc/robot/challenges";
export const BOOK_URL =
	"https://file.tavsys.net/control/controls-engineering-in-frc.pdf";

export function challengeFile(challenge: Challenge): string {
	return `${PACKAGE_DIR}/${challenge.className}.java`;
}

const WPILIB = "https://docs.wpilib.org/en/stable/docs/software/advanced-controls";
const doc = (title: string, page: string): DocLink => ({
	title: `WPILib: ${title}`,
	url: `${WPILIB}/${page}.html`,
});

const DOCS = {
	pidIntro: doc("Introduction to PID", "introduction/introduction-to-pid"),
	pidController: doc("PID Control in WPILib", "controllers/pidcontroller"),
	feedforward: doc("Feedforward Control in WPILib", "controllers/feedforward"),
	bangBang: doc("Bang-Bang Control", "controllers/bang-bang"),
	tuneFlywheel: doc(
		"Tuning a Flywheel Velocity Controller",
		"introduction/tuning-flywheel",
	),
	tuneTurret: doc(
		"Tuning a Turret Position Controller",
		"introduction/tuning-turret",
	),
	tuneArm: doc(
		"Tuning a Vertical Arm Position Controller",
		"introduction/tuning-vertical-arm",
	),
	controlIssues: doc(
		"Common Control Loop Tuning Issues",
		"introduction/common-control-issues",
	),
	profiles: doc(
		"Trapezoidal Motion Profiles",
		"controllers/trapezoidal-profiles",
	),
	sysid: doc("System Identification", "system-identification/index"),
	stateSpace: doc(
		"Introduction to State-Space Control",
		"state-space/state-space-intro",
	),
	flywheelWalkthrough: doc(
		"State-Space Flywheel Walkthrough",
		"state-space/state-space-flywheel-walkthrough",
	),
	observers: doc("State Observers and Kalman Filters", "state-space/state-space-observers"),
	filters: doc("Linear Filters", "filters/linear-filter"),
	swerve: {
		title: "WPILib: Swerve Drive Kinematics (module state optimization)",
		url: "https://docs.wpilib.org/en/stable/docs/software/kinematics-and-odometry/swerve-drive-kinematics.html",
	},
};

const CALCULATE: CodeRef = {
	code: "public double calculate(Mechanism flywheel)",
	text: "Runs every 20 ms. Return the motor's voltage, from -12 to 12.",
};
const VELOCITY: CodeRef = {
	code: "flywheel.velocity()",
	text: "The measured speed, in RPM.",
};
const GOAL: CodeRef = {
	code: "flywheel.goal()",
	text: "The speed the trial wants right now, in RPM. It changes during the trial.",
};
const TUNABLE: CodeRef = {
	code: "kP.get()",
	text: "A tunable number's current value: whatever its slider says.",
};
const PID_CALCULATE: CodeRef = {
	code: "pid.calculate(measurement, goal)",
	text: "WPILib's PID output. The measurement comes **first**.",
};

const FLYWHEEL_FF_SLIDERS: Record<string, Slider> = {
	kS: { min: 0, max: 1, step: 0.01, unit: "V" },
	kV: { min: 0, max: 0.004, step: 0.00001, unit: "V/RPM" },
	kP: { min: 0, max: 0.1, step: 0.0005, unit: "V/RPM" },
};
const STEERING_SLIDERS: Record<string, Slider> = {
	kP: { min: 0, max: 1, step: 0.005, unit: "V/°" },
	kD: { min: 0, max: 0.05, step: 0.0005, unit: "V/(°/s)" },
};
const ELEVATOR_SLIDERS: Record<string, Slider> = {
	kP: { min: 0, max: 150, step: 1, unit: "V/m" },
	kI: { min: 0, max: 300, step: 1, unit: "V/(m·s)" },
	kD: { min: 0, max: 10, step: 0.1, unit: "V/(m/s)" },
	kG: { min: 0, max: 1.5, step: 0.01, unit: "V" },
};

// ---------------------------------------------------------------------------

export const CHALLENGES: Challenge[] = [
	{
		id: "open-loop",
		number: 0,
		title: "Example: Open Loop",
		className: "Challenge00OpenLoop",
		level: "Start here",
		mechanism: "flywheel",
		summary:
			"A finished example, so you can see how the lab works. It gives a shooter flywheel a fixed 6 volts and never checks its speed.",
		learn: ["Running a trial", "Plant, input and output", "Open-loop control"],
		idea: [
			"Every control problem has a **plant** (the mechanism you control: here a shooter flywheel), an **input** you choose (the motor's voltage, `u`), and an **output** you care about (its speed, `y`). The **goal**, or reference `r`, is the output you want.",
			"This controller is **open-loop**: it picks the voltage without ever looking at the output. It works only as well as your guess, and it can't notice when something slows the flywheel down.",
		],
		steps: [
			"Click **Start** in the Driver Station and wait for the dot at the top of this tab to turn green.",
			"Pick **Example: Open Loop** and click **Run trial**. Watch the flywheel spin up and the plot draw its speed.",
			"Read the result: the flywheel settles near 2900 RPM, short of the 3000 RPM goal (the dashed line).",
			"Open the file and change `6.0` to `12.0`. Save, then click **Restart** in the Driver Station. The trial runs again by itself. How fast does it go now?",
			"Click **Sandbox** to play: you set the goal, and you can shoot balls through the flywheel.",
		],
		code: [
			CALCULATE,
			{
				code: "return 6.0;",
				text: "The voltage for the motor. This controller never reads `flywheel.velocity()`: that's what makes it open-loop.",
			},
		],
		hints: [
			"The top plot shows the goal (dashed) and the flywheel's speed. The bottom plot shows the voltage your code returned.",
			"The speed curve bends over as it nears its top speed: a **first-order** response. The flywheel speeds up fast at first, then slower as friction and the motor's back-EMF catch up with the voltage.",
		],
		book: [
			{ section: "1.1", title: "What is gain?" },
			{ section: "1.3", title: "Open-loop and closed-loop systems" },
		],
		docs: [DOCS.pidIntro],
	},
	{
		id: "bang-bang",
		number: 1,
		title: "Bang-Bang",
		className: "Challenge01BangBang",
		level: "Feedback basics",
		mechanism: "flywheel",
		summary:
			"Full power when too slow, nothing when fast enough. Get the flywheel to 3000 RPM, then 4500 RPM.",
		learn: ["Closed-loop control", "Error", "if / else"],
		idea: [
			"A **closed-loop** (feedback) controller measures the output and reacts to the **error**, `e = r − y`: how far the output is from the goal.",
			"**Bang-bang** is the simplest feedback: all or nothing. It is a real option for flywheels (WPILib has a `BangBangController`): it gets up to speed as fast as physically possible. The price is a voltage that chatters between 0 and 12 V, and a speed that ripples around the goal.",
		],
		steps: [
			"In `calculate`, compare `flywheel.velocity()` with `flywheel.goal()`.",
			"Return `12.0` when the flywheel is slower than the goal, and `0.0` otherwise.",
			"Save, click **Restart**, then **Run trial**. Look at the voltage: it flips between 0 and 12 every few loops.",
			"Why does the speed ripple more at 4500 RPM than at 3000?",
		],
		code: [
			CALCULATE,
			VELOCITY,
			GOAL,
			{
				code: "if (a < b) {\n  return 12.0;\n}\nreturn 0.0;",
				text: "Return one value or the other depending on a comparison.",
			},
		],
		hints: [
			"The controller only decides every 20 ms. Between decisions the flywheel keeps speeding up (or slowing down), so it always overshoots a little.",
			"At 4500 RPM, the flywheel slows faster when coasting (more back-EMF and friction to fight), so each 20 ms of coasting drops more speed.",
			"Why not return -12 when too fast? Driving a flywheel backwards to slow it down wastes energy and hammers the gearbox. Coasting is enough.",
		],
		book: [
			{ section: "1.3", title: "Open-loop and closed-loop systems" },
			{ section: "1.5", title: "Why feedback control?" },
		],
		docs: [DOCS.bangBang],
	},
	{
		id: "proportional",
		number: 2,
		title: "Proportional Control",
		className: "Challenge02Proportional",
		level: "Feedback basics",
		mechanism: "flywheel",
		summary:
			"Push in proportion to the error. Tune kP to get close to 3000 RPM without the voltage chattering.",
		learn: ["P controller", "Tuning a gain", "Steady-state error"],
		idea: [
			"A **P controller** makes the voltage proportional to the error: `u = kP · e`. Big error, big push; small error, small push. The book calls it a *software-defined spring* pulling the output toward the goal.",
			"But a flywheel needs voltage just to keep spinning, and a P controller only makes voltage when there is error. So it settles where `kP · e` is just enough to hold the speed, a little short of the goal. That leftover is **steady-state error**.",
			"Turn kP up and the error shrinks, until the controller overreacts. The robot only updates every 20 ms, so a huge kP over-corrects between updates and the voltage starts to chatter. Every gain is a trade-off.",
		],
		steps: [
			"Work out `double error = flywheel.goal() - flywheel.velocity();` and return `kP.get() * error`.",
			"Save, **Restart**, then **Run trial** with kP at 0. Nothing moves: no gain, no push.",
			"Move the **kP** slider and run again. Try 0.005, 0.02, 0.05, 0.08. What happens to the error? And to the voltage?",
			"Find a kP that passes both checks. Then go for the bonus star.",
		],
		code: [VELOCITY, GOAL, TUNABLE],
		hints: [
			"The error is in RPM and the answer is in volts, so kP is in volts per RPM: a small number. With kP = 0.004, 3000 RPM of error asks for 12 V.",
			"With kP = 0.01 the flywheel settles near 2500 RPM. There it needs about 5 V to keep spinning, and 0.01 × 500 RPM of error is 5 V: exactly enough, so it stays there.",
			"Above about kP = 0.06, the voltage flips up and down every loop: the robot is correcting an error it already over-corrected. That's the limit of this controller at 50 updates a second.",
			"The bonus needs kP between about 0.04 and 0.055.",
		],
		book: [
			{ section: "2.1", title: "Proportional term" },
			{ section: "2.5", title: "Response types" },
			{ section: "7.2", title: "Effects of discretization on controller performance" },
		],
		docs: [DOCS.pidIntro, DOCS.tuneFlywheel],
		sliders: { kP: { min: 0, max: 0.1, step: 0.0005, unit: "V/RPM" } },
	},
	{
		id: "feedforward",
		number: 3,
		title: "Feedforward",
		className: "Challenge03Feedforward",
		level: "Feedback basics",
		mechanism: "flywheel",
		summary:
			"Predict the voltage the flywheel needs, and let a P controller fix the rest. Hold speed through three shots.",
		learn: ["Feedforward", "kS and kV", "Rejecting disturbances"],
		idea: [
			"Feedback is reactive: it only acts once the flywheel is already wrong. A **feedforward** uses what you know about the mechanism to work out the voltage it needs *before* anything goes wrong.",
			"A motor at a steady speed needs a voltage proportional to that speed (to cancel its back-EMF) plus a bit to overcome friction: `u = kS · sign(v) + kV · v`. Feed the *goal* speed in, and the flywheel gets the right voltage from the start.",
			"Feedback then has little left to do: only what the model misses, like the drag of a ball squeezing through the shooter. Feedforward plus feedback is how almost every FRC mechanism is controlled.",
		],
		steps: [
			"Return `kS.get() * Math.signum(goal) + kV.get() * goal + kP.get() * error`.",
			"Save and **Restart**. Set kS and kP to 0, and tune **kV** alone until the flywheel holds about 3000 RPM.",
			"Now check 4500 RPM (the second goal). If it's off there, kS is missing: raise kS and lower kV until both speeds are right.",
			"Bring kP back to recover quickly from the three shots. Watch the dip after each ball.",
		],
		code: [
			GOAL,
			VELOCITY,
			{ code: "Math.signum(x)", text: "1 if `x` is positive, -1 if negative, 0 if zero." },
			{
				code: "new SimpleMotorFeedforward(kS, kV).calculate(goal)",
				text: "WPILib's version of the same feedforward (`edu.wpi.first.math.controller`).",
			},
		],
		hints: [
			"The flywheel's top speed is about 5900 RPM at 12 V, so roughly 12 / 5900 ≈ 0.002 volts per RPM.",
			"kS is the voltage it takes just to get the flywheel turning. Try it in **Sandbox**: set a small goal and see when the flywheel starts to move.",
			"The real values are kS = 0.25 V and kV = 0.002 V/RPM. A kP of 0.01 to 0.03 recovers quickly from shots.",
		],
		book: [
			{ section: "1.4", title: "Feedforward" },
			{ section: "7.8", title: "Feedforward" },
			{ section: "6.10.7", title: "Do flywheels need PD control?" },
			{ section: "C", title: "Feedforwards" },
		],
		docs: [DOCS.feedforward, DOCS.tuneFlywheel],
		sliders: FLYWHEEL_FF_SLIDERS,
	},
	{
		id: "pd-steering",
		number: 4,
		title: "PD Steering",
		className: "Challenge04PDSteering",
		level: "Position control",
		mechanism: "steering",
		summary:
			"Point a swerve module's wheel at 90°, −45° and 30°: quickly, and without overshooting by more than 5°.",
		learn: ["Position control", "Derivative term", "Fields that remember"],
		idea: [
			"Controlling a *position* is different from a speed: the wheel has to stop exactly at the goal, and its inertia wants to carry it past. With P alone it swings past and back, like a mass on a spring.",
			"The **derivative** term adds a damper, `kD · de/dt`, where `de/dt` is how fast the error is changing. Rushing toward the goal makes the error shrink fast, so the D term brakes before arriving. The book shows D is really a P controller on velocity: it drives the velocity error to zero.",
			"The book's recipe for tuning a position controller: raise kP until it starts to oscillate, then raise kD to damp it out.",
		],
		steps: [
			"Add a field `private double lastError;`. In `reset`, set it to `wheel.goal() - wheel.position()`.",
			"In `calculate`, work out the error and `double derivative = (error - lastError) / wheel.dt();`, then save `lastError = error;`.",
			"Return `kP.get() * error + kD.get() * derivative`.",
			"Save, **Restart**, **Run trial**. With kD = 0, how big can kP get before it overshoots?",
			"Raise kD until the overshoot is gone, then raise kP again for speed.",
		],
		code: [
			{ code: "wheel.position()", text: "The wheel's measured angle, in degrees, from -180 to 180." },
			{ code: "wheel.goal()", text: "The angle the trial wants." },
			{ code: "wheel.dt()", text: "Time between loops: 0.02 seconds." },
		],
		hints: [
			"Angles are in degrees, so kP is in volts per degree. At kP = 0.1, a 90° error asks for 9 V.",
			"Try kP = 0.2 with kD = 0: fast, but it overshoots by more than 30°. Now add kD = 0.01.",
			"Why set lastError in `reset`? Otherwise on the first loop lastError is 0, the error is 90°, and the derivative is 90 / 0.02 = 4500: a huge kick. That's called **derivative kick**.",
			"Another way to write D: `-kD.get() * wheel.velocity()`. When the goal isn't moving it's the same thing, with no derivative kick.",
		],
		book: [
			{ section: "2.2", title: "Derivative term" },
			{ section: "2.5", title: "Response types" },
			{ section: "2.6", title: "Manual tuning" },
		],
		docs: [DOCS.tuneTurret, DOCS.pidIntro],
		sliders: STEERING_SLIDERS,
	},
	{
		id: "shortest-path",
		number: 5,
		title: "Shortest Path",
		className: "Challenge05ShortestPath",
		level: "Position control",
		mechanism: "steering",
		summary:
			"Angles wrap around at ±180°. Take the short way every time, using WPILib's PIDController.",
		learn: ["WPILib PIDController", "Continuous input", "Wrap-around angles"],
		idea: [
			"An angle sensor reports −180° to 180°, and those two are the same direction. To go from 150° to −150°, plain subtraction says the error is −300°, so the wheel turns 300° the long way round, when 60° the other way would do.",
			"The fix is to wrap the error into −180° to 180° before using it. WPILib's `PIDController` does that with `enableContinuousInput(-180, 180)`. Real swerve modules steer this way, and also reverse the wheel's drive direction so they never turn more than 90°.",
		],
		steps: [
			"In `reset`, call `pid.enableContinuousInput(-180, 180);`.",
			"In `calculate`, return `pid.calculate(wheel.position(), wheel.goal())`.",
			"Save, **Restart**, and copy your kP and kD from PD Steering onto the sliders. **Run trial**.",
			"Try it once without `enableContinuousInput` and watch it go the long way round.",
		],
		code: [
			PID_CALCULATE,
			{
				code: "pid.enableContinuousInput(-180, 180);",
				text: "Tells the controller the input wraps around, so it always takes the short way.",
			},
			{
				code: "MathUtil.inputModulus(value, -180, 180)",
				text: "Wraps any angle into -180 to 180, if you'd like to do it yourself.",
			},
		],
		hints: [
			"Your gains from PD Steering should work almost unchanged: PIDController does the same math.",
			"Without continuous input the wheel turns about 750° in total. With it, about 330°.",
		],
		book: [{ section: "2.4", title: "PID controller definition" }],
		docs: [DOCS.pidController, DOCS.swerve],
		sliders: STEERING_SLIDERS,
	},
	{
		id: "fight-gravity",
		number: 6,
		title: "Fight Gravity",
		className: "Challenge06FightGravity",
		level: "Position control",
		mechanism: "elevator",
		summary: "Hold an elevator at 0.5 m, 1.2 m and 0.3 m, within 1 cm.",
		learn: ["Gravity feedforward", "Known forces", "WPILib PIDController"],
		idea: [
			"Gravity pulls an elevator's carriage down with the same force at every height. To hold still, the motor must push back with a constant voltage, **kG**. A PID controller can only make that voltage from error, so the carriage sags until the error is big enough: about `kG / kP`.",
			"Add kG as a feedforward and the PID starts from a carriage that already floats. This is the book's second kind of feedforward: it cancels a force you know about, so feedback doesn't have to.",
		],
		steps: [
			"Return `pid.calculate(elevator.position(), elevator.goal()) + kG.get()`.",
			"Save, **Restart**, **Run trial** with kG = 0 first. Tune kP and kD: the carriage never quite reaches its goals.",
			"Find kG in **Sandbox**: set kP and kD to 0 and raise kG until the carriage stops sliding down.",
			"With kG right, bring kP and kD back and pass the checks.",
		],
		code: [
			PID_CALCULATE,
			{ code: "elevator.position()", text: "The carriage's measured height, in meters. 0 is the bottom." },
		],
		hints: [
			"Heights are in meters, so kP is in volts per meter. With kP = 40, 1 cm of error is 0.4 V.",
			"kG is close to 0.45 V. Too big, and the carriage floats up instead.",
			"kP = 40, kD = 2 is a good start. For the bonus, try kP = 100, kD = 6.",
		],
		book: [
			{ section: "6.9.3", title: "Gravity feedforward" },
			{ section: "12.2", title: "Elevator" },
			{ section: "1.4", title: "Feedforward" },
		],
		docs: [DOCS.feedforward, DOCS.pidController],
		sliders: ELEVATOR_SLIDERS,
	},
	{
		id: "mystery-payload",
		number: 7,
		title: "Mystery Payload",
		className: "Challenge07MysteryPayload",
		level: "Position control",
		mechanism: "elevator",
		summary:
			"Two seconds in, the elevator grabs a game piece of unknown weight. Fix the sag with the integral term, without winding up.",
		learn: ["Integral term", "Integral windup", "I-zone"],
		idea: [
			"Your kG was tuned for an empty carriage. With a game piece on board gravity pulls harder, and the carriage sags again. You can't feedforward what you don't know.",
			"The **integral** term adds up the error over time, `kI · ∫e dt`, and pushes harder the longer an error lasts. Any steady sag keeps growing the integral until the push is enough and the error is gone.",
			"The catch is **windup**: during a big move the error is large for a long time, so the integral piles up a huge push and flings the carriage past its goal. The fix is to integrate only near the goal, an **I-zone**. The book's advice: prefer feedforward, and save the integral for what you can't model.",
		],
		steps: [
			"Return `pid.calculate(elevator.position(), elevator.goal()) + kG.get()`, and copy kP, kD and kG from Fight Gravity.",
			"**Run trial** with kI = 0: the payload arrives at 2 s and the carriage sags about 1 cm.",
			"Raise kI until the sag disappears within 1.5 s. What happens to the first move?",
			"In `reset`, call `pid.setIZone(0.03);` so the integral only works within 3 cm of the goal. Pass all four checks.",
		],
		code: [
			PID_CALCULATE,
			{
				code: "pid.setIZone(0.03);",
				text: "Clears the integral whenever the error is bigger than 0.03 m.",
			},
			{
				code: "pid.setIntegratorRange(-1.0, 1.0);",
				text: "Limits how many volts the integral can add (±1 V is WPILib's default).",
			},
		],
		hints: [
			"Without an I-zone, kI = 60 overshoots the first move by almost 3 cm.",
			"With `pid.setIZone(0.03)`, kI anywhere from about 60 to 250 works.",
			"The payload needs only about 0.35 V more, well inside the default ±1 V integrator range.",
		],
		book: [
			{ section: "2.3", title: "Integral term" },
			{ section: "2.6", title: "Manual tuning (integral windup)" },
			{ section: "6.7", title: "Integral control" },
		],
		docs: [DOCS.controlIssues, DOCS.pidController],
		sliders: ELEVATOR_SLIDERS,
	},
	{
		id: "arm-feedforward",
		number: 8,
		title: "Arm Feedforward",
		className: "Challenge08ArmFeedforward",
		level: "Position control",
		mechanism: "arm",
		summary:
			"Move an arm to 0°, 90° and 30°. Gravity's pull changes with the angle, so the feedforward must too.",
		learn: ["Arm feedforward", "Radians", "Math.cos"],
		idea: [
			"Gravity's torque on an arm depends on its angle. Sticking straight out (0 rad), its whole weight hangs off the pivot. Pointing straight up (π/2 rad), gravity pulls straight through the pivot and the arm needs no holding at all.",
			"The torque follows the cosine of the angle, so the gravity feedforward is `kG · cos(θ)`. A constant kG, like the elevator's, is too strong when the arm points up and pushes it past its goal.",
		],
		steps: [
			"Return `pid.calculate(arm.position(), arm.goal()) + kG.get() * Math.cos(arm.position())`.",
			"Save, **Restart**. Find kG in **Sandbox**: with kP and kD at 0 and the goal at 0°, raise kG until the arm stops sagging.",
			"Tune kP and kD for the checks.",
			"Try a constant kG instead of the cosine. Which goal goes wrong?",
		],
		code: [
			{
				code: "arm.position()",
				text: "The arm's angle in **radians**: 0 is straight out, π/2 ≈ 1.571 is straight up.",
			},
			{ code: "Math.cos(radians)", text: "Cosine. Java's trig functions all use radians." },
			{
				code: "Math.toRadians(90)",
				text: "Converts degrees to radians (`Math.toDegrees` goes the other way).",
			},
			{
				code: "new ArmFeedforward(kS, kG, kV).calculate(angle, velocity)",
				text: "WPILib's arm feedforward, with the cosine built in.",
			},
		],
		hints: [
			"kG is about 0.9 V.",
			"kP = 15, kD = 1 is a good start. For the bonus try kP = 25, kD = 1.5.",
		],
		book: [
			{ section: "6.11.3", title: "Gravity feedforward" },
			{ section: "12.4", title: "Single-jointed arm" },
		],
		docs: [DOCS.tuneArm, DOCS.feedforward],
		sliders: {
			kP: { min: 0, max: 60, step: 0.5, unit: "V/rad" },
			kD: { min: 0, max: 5, step: 0.05, unit: "V/(rad/s)" },
			kG: { min: 0, max: 2, step: 0.01, unit: "V" },
		},
	},
	{
		id: "motion-profile",
		number: 9,
		title: "Motion Profile",
		className: "Challenge09MotionProfile",
		level: "Motion profiles",
		mechanism: "elevator",
		summary:
			"Move the elevator 1.2 m smoothly: under 1.5 m/s, no jerky starts, and no overshoot.",
		learn: ["Trapezoid profiles", "Actuator saturation", "Following a moving setpoint"],
		idea: [
			"Give a PID controller a goal 1.2 m away and it asks for dozens of volts. The battery only has 12, so the controller **saturates**: it acts as if its gain were smaller, and you lose control of how the move happens. The carriage slams up at full speed.",
			"A **motion profile** plans the move instead: speed up at a fixed rate, cruise, then slow down and stop exactly on the goal. Its velocity plot is a trapezoid. Each loop, the controller follows the profile's *current* position and velocity, not the final goal.",
			"Following a moving setpoint needs a feedforward for velocity and acceleration too: `u = kS + kG + kV · v + kA · a`. WPILib's `ElevatorFeedforward` works it out from one profile step to the next.",
		],
		steps: [
			"Add fields `private TrapezoidProfile profile;` and `private TrapezoidProfile.State setpoint;`.",
			"In `reset`, make the profile: `profile = new TrapezoidProfile(new TrapezoidProfile.Constraints(maxVelocity.get(), maxAcceleration.get()));` and start `setpoint = new TrapezoidProfile.State(elevator.position(), 0);`.",
			"In `calculate`, step the profile: `var next = profile.calculate(elevator.dt(), setpoint, new TrapezoidProfile.State(elevator.goal(), 0));`.",
			"Work out `feedforward.calculateWithVelocities(setpoint.velocity, next.velocity) + pid.calculate(elevator.position(), next.position)`, then set `setpoint = next;` and return the voltage.",
			"Draw the plan with `elevator.plot(\"Setpoint\", setpoint.position);` to see how closely the elevator follows it.",
		],
		code: [
			{
				code: "profile.calculate(dt, current, goal)",
				text: "Where the profile says to be one step (`dt`) from now, as a `TrapezoidProfile.State` with `.position` and `.velocity`.",
			},
			{
				code: "feedforward.calculateWithVelocities(now, next)",
				text: "Voltage to go from one velocity to the next in one loop, with kS and kG included.",
			},
			{ code: "elevator.plot(name, value)", text: "Draws your own line on the plot." },
		],
		hints: [
			"Before you start, try plain PID + kG here: it arrives, but at 2 m/s and 19 m/s².",
			"maxVelocity = 1.2 m/s and maxAcceleration = 4 m/s² work well, with kP = 30 and kD = 1.",
			"Overshooting a little? Your PID chases `next.position`, 20 ms ahead of where the elevator should be now. Compare against `setpoint.position` (before you update it) for the bonus.",
			"The book's tip (2.6): with a profile, tune kP until the position tracks, then kD until the velocity tracks.",
		],
		book: [
			{ section: "15.1", title: "1-DOF motion profiles" },
			{ section: "3.2", title: "Actuator saturation" },
			{ section: "2.6", title: "Manual tuning" },
		],
		docs: [DOCS.profiles, DOCS.feedforward],
		sliders: {
			kP: { min: 0, max: 100, step: 1, unit: "V/m" },
			kD: { min: 0, max: 10, step: 0.1, unit: "V/(m/s)" },
			maxVelocity: { min: 0.1, max: 2.5, step: 0.05, unit: "m/s" },
			maxAcceleration: { min: 0.5, max: 20, step: 0.25, unit: "m/s²" },
		},
	},
	{
		id: "sysid",
		number: 10,
		title: "Mystery Flywheel (SysId)",
		className: "Challenge10SysId",
		level: "Models and LQR",
		mechanism: "flywheel",
		summary:
			"Nobody knows this flywheel's kS, kV or kA. Design the test voltages that let the lab measure them.",
		learn: ["System identification", "Quasistatic and dynamic tests", "Least squares"],
		idea: [
			"Feedforwards and state-space controllers need numbers: kS, kV and kA. You can estimate them from motor datasheets, but measuring the real mechanism is better. That's **system identification**.",
			"The model `u = kS · sign(v) + kV · v + kA · a` has three unknowns. Record the voltage and speed while the flywheel runs, and **least squares** finds the constants that best explain the data, like fitting a line through points. The book shows how, one 20 ms step at a time.",
			"The data must show each term at work. A slow **quasistatic** ramp keeps acceleration near zero, which pins down kS and kV. A sudden **dynamic** step makes the flywheel accelerate hard, which pins down kA. WPILib's SysId tool runs both.",
		],
		steps: [
			"In `calculate`, return a voltage that depends on `t`. Start with just a ramp: `return 1.5 * t;`.",
			"Save, **Restart**, **Run trial**. With only a ramp, the lab can't tell kV and kA apart.",
			"Add a dynamic step: for example, ramp for 4 s, then jump to 10 V, then drop to 2 V.",
			"When all three checks pass, **write down kS, kV and kA**. You'll need them for LQR and Kalman Filter.",
		],
		code: [
			{ code: "flywheel.time()", text: "Seconds since the trial started: 0 to 8." },
			{
				code: "if (t < 4) {\n  return 1.5 * t;\n} else if (t < 6) {\n  …\n}",
				text: "Different voltages for different parts of the test.",
			},
		],
		hints: [
			"Keep the flywheel moving: the lab ignores data where it's almost stopped, because sticky friction breaks the model there.",
			"kA is the hard one. It only shows while the speed is changing, so big, sudden voltage changes help.",
			"A test that works: `if (t < 4) return 1.5 * t; if (t < 6) return 10.0; return 2.0;`",
		],
		book: [
			{ section: "14.1", title: "Ordinary least squares" },
			{ section: "14.2.1", title: "Simple system identification" },
			{ section: "12.3", title: "Flywheel" },
		],
		docs: [DOCS.sysid],
	},
	{
		id: "lqr",
		number: 11,
		title: "LQR",
		className: "Challenge11LQR",
		level: "Models and LQR",
		mechanism: "flywheel",
		summary:
			"Let a linear-quadratic regulator pick your feedback gain from the model you measured.",
		learn: ["State-space models", "LQR and Bryson's rule", "Model-based design"],
		idea: [
			"Your SysId constants make a **state-space model** of the flywheel: `dv/dt = −(kV/kA)·v + (1/kA)·u`. It predicts how the speed (the **state**) changes for any voltage.",
			"An **LQR** uses that model to find the gain K that minimizes a cost: error squared plus voltage squared, each with a weight. You pick the weights with **Bryson's rule**: the largest error you'd accept, and the largest voltage you'd like to use. A small error tolerance makes it aggressive; a small voltage tolerance makes it gentle.",
			"The LQR only does feedback. Add the feedforward `kS + kV · r` so the LQR handles just the error, like kP did in Feedforward, except the gain now comes from the model.",
		],
		steps: [
			"Put your SysId kS, kV and kA in the constants at the top.",
			"In `reset`, make the LQR: `lqr = new LinearQuadraticRegulator<>(plant, VecBuilder.fill(maxError.get()), VecBuilder.fill(maxVolts.get()), flywheel.dt());`",
			"In `calculate`, with `double r = flywheel.goal();`, the feedback is `lqr.calculate(VecBuilder.fill(flywheel.velocity()), VecBuilder.fill(r)).get(0, 0)`. Add `kS * Math.signum(r) + kV * r`, and clamp.",
			"Save, **Restart**, **Run trial**. Change maxError and run again: watch how the response changes.",
			"Bonus: find tolerances that pass every check without ever asking for 12 V.",
		],
		code: [
			{
				code: "VecBuilder.fill(x)",
				text: "A 1×1 vector holding `x`. WPILib's state-space classes work with vectors and matrices.",
			},
			{
				code: "lqr.calculate(x, r).get(0, 0)",
				text: "The LQR's voltage to move state `x` toward reference `r`.",
			},
			{
				code: "lqr.getK().get(0, 0)",
				text: "The gain the LQR chose: a kP, in volts per RPM.",
			},
			{
				code: "MathUtil.clamp(volts, -12.0, 12.0)",
				text: "Keeps a value inside a range.",
			},
		],
		hints: [
			"Print the gain in `reset` with `System.out.println(lqr.getK());` and compare it with the kP you tuned by hand in Feedforward.",
			"maxError = 100 RPM is aggressive: the voltage hits 12 V on every step. Try 1000, 3000 and 3500.",
			"Stuck on SysId? The mystery flywheel is close to kS = 0.3, kV = 0.00176, kA = 0.0004.",
			"The book also covers `lqr.latencyCompensate(plant, dt, delay)`, for sensors that report late (appendix B.5).",
		],
		book: [
			{ section: "6.3", title: "Continuous state-space notation" },
			{ section: "6.10", title: "Flywheel" },
			{ section: "7.7", title: "Linear-quadratic regulator (7.7.3: Bryson's rule)" },
			{ section: "7.8", title: "Feedforward" },
		],
		docs: [DOCS.stateSpace, DOCS.flywheelWalkthrough],
		sliders: {
			maxError: { min: 5, max: 10000, step: 1, log: true, unit: "RPM" },
			maxVolts: { min: 0.5, max: 12, step: 0.5, unit: "V" },
		},
	},
	{
		id: "noisy-sensor",
		number: 12,
		title: "Noisy Sensor",
		className: "Challenge12NoisySensor",
		level: "Noise and estimation",
		mechanism: "flywheel",
		summary:
			"This speed sensor is off by about 120 RPM on every reading. Filter it without falling behind.",
		learn: ["Measurement noise", "Low-pass filters", "Filter lag"],
		idea: [
			"Real sensors are noisy. Feed the noise straight into a P controller and it turns into shaky voltage, `kP × 120 RPM` of jitter every loop. That heats and wears the motor, and makes the flywheel less steady, not more.",
			"A **low-pass filter** averages recent readings so random noise cancels out. WPILib's `singlePoleIIR` filter moves its output part of the way toward each new reading. Its *time constant* is how long it takes to catch up.",
			"Averaging has a price: **lag** (the book calls it phase loss). A heavily filtered speed notices a ball's dip late, so the controller reacts late. Tune the time constant for the least noise you can get while still recovering from shots.",
		],
		steps: [
			"Copy your kS, kV and kP from Feedforward: it's the same flywheel.",
			"In `calculate`, filter the reading: `double speed = filter.calculate(flywheel.velocity());`, then `flywheel.showEstimate(speed);`.",
			"Use `speed` instead of `flywheel.velocity()` in your feedforward + P controller.",
			"Save, **Restart**, **Run trial**. Change **timeConstant** and run again. Too small: noisy. Too big: slow to recover.",
		],
		code: [
			{
				code: "LinearFilter.singlePoleIIR(timeConstant, dt)",
				text: "A low-pass filter. Bigger time constant: smoother, but slower.",
			},
			{
				code: "filter.calculate(reading)",
				text: "Feeds in one reading and returns the filtered value. Call it once per loop.",
			},
			{
				code: "flywheel.showEstimate(speed)",
				text: "Draws your filtered speed as **Your estimate** and grades it against the true speed.",
			},
		],
		hints: [
			"The plot draws the raw sensor faintly, your estimate, and the true speed. Compare them.",
			"timeConstant from about 0.08 to 0.12 s, with kP from 0.005 to 0.01, passes.",
			"The filter is made in `reset`, so a new timeConstant takes effect on the next run.",
		],
		book: [
			{ section: "E.6", title: "Phase loss" },
			{ section: "9.1", title: "Terminology" },
			{ section: "6.10.7", title: "Do flywheels need PD control?" },
		],
		docs: [DOCS.filters],
		sliders: {
			...FLYWHEEL_FF_SLIDERS,
			timeConstant: { min: 0.001, max: 0.5, step: 0.001, unit: "s" },
		},
	},
	{
		id: "kalman-filter",
		number: 13,
		title: "Kalman Filter",
		className: "Challenge13KalmanFilter",
		level: "Noise and estimation",
		mechanism: "flywheel",
		summary:
			"The mystery flywheel with the terrible sensor. Use its model to estimate the speed without lag.",
		learn: ["State observers", "Kalman filter", "Predict and correct"],
		idea: [
			"A low-pass filter only has the readings to go on, so it must lag. But you know more: your SysId model predicts how the speed changes for the voltage you applied. A **state observer** runs that model alongside the real flywheel and nudges its estimate toward each measurement.",
			"The **Kalman filter** decides how hard to nudge by comparing how much it trusts the model (the model's standard deviation) with how much it trusts the sensor. Every loop it **predicts** with the voltage, then **corrects** with the measurement.",
			"When the goal jumps, a low-pass filter's estimate trails behind. The Kalman filter knows you just applied 12 V, predicts the speed-up, and stays with the truth.",
		],
		steps: [
			"Put your SysId constants at the top.",
			"In `reset`, make the filter: `observer = new KalmanFilter<>(Nat.N1(), Nat.N1(), plant, VecBuilder.fill(modelStdDev.get()), VecBuilder.fill(sensorStdDev.get()), flywheel.dt());`",
			"In `calculate`, predict with last loop's voltage minus what friction used: `double u = flywheel.appliedVoltage() - kS * Math.signum(observer.getXhat(0));` then `observer.predict(VecBuilder.fill(u), flywheel.dt());`.",
			"Correct with this loop's reading: `observer.correct(VecBuilder.fill(u), VecBuilder.fill(flywheel.velocity()));`.",
			"Take `double speed = observer.getXhat(0);`, show it, and use it in feedforward + kP. Tune **modelStdDev**.",
		],
		code: [
			{
				code: "observer.predict(u, dt)",
				text: "Moves the estimate forward one loop using the model and the voltage `u`.",
			},
			{
				code: "observer.correct(u, y)",
				text: "Nudges the estimate toward the measurement `y`.",
			},
			{ code: "observer.getXhat(0)", text: "The estimated speed." },
			{
				code: "flywheel.appliedVoltage()",
				text: "The voltage the motor actually got last loop, after clamping to ±12 V.",
			},
		],
		hints: [
			"Try your Noisy Sensor filter here first: it trails about 100 RPM behind every step.",
			"A small modelStdDev trusts the model: smooth, but if your SysId numbers are off, the estimate drifts away. A big one trusts the sensor: noisy. Between 50 and 1000 works with good constants.",
			"Why subtract kS? The model from `identifyVelocitySystem` has no friction in it. Friction uses up kS volts, so give the model only what's left.",
			"WPILib scales the sensor's standard deviation by the loop time, so the numbers don't mean exactly what they say. Tune by watching the plot.",
		],
		book: [
			{ section: "9.2", title: "State observers" },
			{ section: "9.6", title: "Kalman filter" },
			{ section: "9.6.8", title: "Process noise and measurement noise covariance selection" },
		],
		docs: [DOCS.observers],
		sliders: {
			modelStdDev: { min: 1, max: 10000, step: 1, log: true, unit: "RPM" },
			sensorStdDev: { min: 10, max: 1000, step: 1, log: true, unit: "RPM" },
			kP: { min: 0, max: 0.05, step: 0.0005, unit: "V/RPM" },
		},
	},
	{
		id: "state-space-elevator",
		number: 14,
		title: "State-Space Elevator",
		className: "Challenge14StateSpaceElevator",
		level: "Capstone",
		mechanism: "elevator",
		summary:
			"Motion profile, Kalman filter, LQR and feedforward together, on an elevator with a noisy height sensor.",
		learn: ["Two-state models", "LinearSystemLoop", "Putting it all together"],
		idea: [
			"An elevator's state is two numbers, height and velocity: `x = [h, v]`. The model `ẋ = Ax + Bu` predicts both. The only sensor measures height, and it's noisy, so the Kalman filter estimates the velocity too.",
			"The LQR now has two tolerances, height and velocity, and pushes the estimated state toward the profile's setpoint. `LinearSystemLoop` runs the LQR, the Kalman filter and a model-based feedforward together. Gravity and friction aren't in the linear model, so add kG and kS yourself.",
			"This is the structure of the book's elevator example (6.9) and WPILib's StateSpaceElevator example. The **separation principle** says you can design the controller and the observer separately, and they still work together.",
		],
		steps: [
			"LQR: `var lqr = new LinearQuadraticRegulator<>(plant, VecBuilder.fill(maxHeightError.get(), maxVelocityError.get()), VecBuilder.fill(12.0), elevator.dt());`",
			"Kalman filter: `var observer = new KalmanFilter<>(Nat.N2(), Nat.N1(), plant, VecBuilder.fill(0.01, 0.1), VecBuilder.fill(0.004), elevator.dt());`",
			"Loop: `loop = new LinearSystemLoop<>(plant, lqr, observer, 12.0, elevator.dt());` then `loop.reset(VecBuilder.fill(elevator.position(), 0));`. Make the profile and setpoint as in Motion Profile.",
			"Each loop: step the profile, `loop.setNextR(setpoint.position, setpoint.velocity);`, `loop.correct(VecBuilder.fill(elevator.position()));`, show `loop.getXHat(0)`, then `loop.predict(elevator.dt());`.",
			"Return `loop.getU(0) + kG + kS * Math.signum(setpoint.velocity)`.",
		],
		code: [
			{
				code: "loop.setNextR(position, velocity)",
				text: "The state you want next loop: the profile's setpoint.",
			},
			{
				code: "loop.correct(y)",
				text: "Corrects the Kalman filter with the measured height.",
			},
			{
				code: "loop.predict(dt)",
				text: "Works out the next voltage (LQR + feedforward) and predicts the next state.",
			},
			{ code: "loop.getU(0)", text: "The loop's voltage, before your kG and kS." },
			{ code: "loop.getXHat(0)", text: "The estimated height (`getXHat(1)` is the estimated velocity)." },
		],
		hints: [
			"Show the estimate after `correct` but before `predict`: after `predict`, the estimate is for the *next* loop, 20 ms ahead.",
			"The starting tolerances (2 cm, 0.4 m/s) pass. For the bonus, trade a little stiffness for less noise: try 6 cm and 1.2 m/s.",
			"Plot the setpoint and `loop.getXHat(1)` (the estimated velocity) to see what the loop is thinking.",
		],
		book: [
			{ section: "6.9", title: "Elevator" },
			{ section: "6.3", title: "Continuous state-space notation" },
			{ section: "9.2.2", title: "Separation principle" },
			{ section: "9.6", title: "Kalman filter" },
			{ section: "15.1", title: "1-DOF motion profiles" },
		],
		docs: [DOCS.stateSpace, DOCS.observers],
		sliders: {
			maxHeightError: { min: 0.001, max: 0.3, step: 0.001, log: true, unit: "m" },
			maxVelocityError: { min: 0.01, max: 5, step: 0.01, log: true, unit: "m/s" },
			maxVelocity: { min: 0.1, max: 2.5, step: 0.05, unit: "m/s" },
			maxAcceleration: { min: 0.5, max: 20, step: 0.25, unit: "m/s²" },
		},
	},
];

export function findChallenge(id: string | undefined): Challenge | undefined {
	return CHALLENGES.find((challenge) => challenge.id === id);
}

export function optionLabel(challenge: Challenge): string {
	return challenge.number > 0
		? `${challenge.number}. ${challenge.title}`
		: challenge.title;
}
