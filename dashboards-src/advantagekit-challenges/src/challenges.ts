/**
 * Every challenge's text and which panels it shows. The `id`s must match the
 * ones in the lesson's `Challenges.java`.
 *
 * Each challenge shows only the panels it needs. Text fields may use `backticks`
 * for code, **bold** and *italic*.
 */

export type Level =
	| "Start here"
	| "Outputs"
	| "Inputs and IO layers"
	| "Replay"
	| "Advanced"
	| "Put it together";

export type CodeRef = { code: string; text: string };
export type DocLink = { title: string; url: string };

/** A button the challenge binds, so the panel can draw only those. */
export type ButtonRef = { name: "A" | "B" | "X" | "Y"; does: string };

export type PlotLine = {
	/** `output:<key>`, `input:<key>`, `dashboard:<key>` or `truth:<rpm|yawDeg|pivotDeg>`. */
	id: string;
	label: string;
	color: string;
	dashed?: boolean;
};
export type PlotSpec = { title: string; unit?: string; lines: PlotLine[]; range?: [number, number] };

/** A key the student should see in the log view, even before it's there. */
export type ExpectedKey = { key: string; kind: "input" | "output" | "dashboard" };

export type Panels = {
	/** Keys to list first in the log view. Anything else the student logs follows them. */
	log: ExpectedKey[];
	/** Hide these prefixes from the log view, to keep what's new in focus. */
	hide?: string[];
	/** Show which keys are inputs and which are outputs. */
	showKinds?: boolean;
	plots?: PlotSpec[];
	buttons?: ButtonRef[];
	/** Free play can put a game piece in the feeder. */
	piece?: boolean;
	/** Draw the logged poses on a field. */
	field?: boolean;
	/** Draw the `CommandsAll/` keys as a timeline. */
	commands?: boolean;
	/** The dashboard's slider and preset picker. */
	tuning?: boolean;
	/** Which keys the replay panel opens with. */
	replayKeys?: string[];
};

export type Challenge = {
	id: string;
	number: number;
	title: string;
	level: Level;
	/** Project-relative files the student edits, the main one first. */
	files: string[];
	/** One or two sentences: what the trial asks for. */
	summary: string;
	/** What happens in the trial, so the student knows what's tested. */
	trial: string;
	learn: string[];
	/** The idea behind it, a paragraph each. */
	idea: string[];
	steps: string[];
	code: CodeRef[];
	/** Revealed one at a time. */
	hints: string[];
	docs: DocLink[];
	panels: Panels;
};

const CHALLENGE_DIR = "src/main/java/frc/robot/challenges";
const file = (name: string) => `${CHALLENGE_DIR}/${name}.java`;

const AKIT = "https://docs.advantagekit.org";
const DOCS = {
	what: { title: "What is AdvantageKit?", url: `${AKIT}/getting-started/what-is-advantagekit/` },
	outputs: { title: "Recording Outputs", url: `${AKIT}/data-flow/recording-outputs/` },
	annotations: { title: "Annotation Logging", url: `${AKIT}/data-flow/recording-outputs/annotation-logging` },
	types: { title: "Supported Types", url: `${AKIT}/data-flow/supported-types` },
	inputs: { title: "Recording Inputs", url: `${AKIT}/data-flow/recording-inputs` },
	io: { title: "IO Interfaces", url: `${AKIT}/data-flow/recording-inputs/io-interfaces` },
	dashboard: { title: "Dashboard Inputs", url: `${AKIT}/data-flow/recording-inputs/dashboard-inputs` },
	replay: { title: "How To: Traditional Replay", url: `${AKIT}/getting-started/traditional-replay` },
	nondeterministic: {
		title: "Non-Deterministic Data Sources",
		url: `${AKIT}/getting-started/common-issues/non-deterministic-data-sources`,
	},
	uninitialized: {
		title: "Uninitialized Inputs",
		url: `${AKIT}/getting-started/common-issues/uninitialized-inputs`,
	},
	threads: { title: "Multithreading", url: `${AKIT}/getting-started/common-issues/multithreading` },
	timestamps: { title: "Deterministic Timestamps", url: `${AKIT}/theory/deterministic-timestamps` },
	odometry: { title: "High-Frequency Odometry", url: `${AKIT}/theory/high-frequency-odometry` },
	cases: { title: "Replay Case Studies", url: `${AKIT}/theory/case-studies` },
	comparison: { title: "Log Replay Comparison", url: `${AKIT}/theory/log-replay-comparison` },
	scope: { title: "AdvantageScope", url: "https://docs.advantagescope.org/" },
	scheduler: {
		title: "WPILib: The Command Scheduler",
		url: "https://docs.wpilib.org/en/stable/docs/software/commandbased/command-scheduler.html",
	},
};

const RUN =
	"Save, click **Restart** in the Driver Station, then **Run trial**. The trial starts when you **Enable** the robot in **Teleop**.";
const REPLAY_AFTER =
	"When the trial ends, the lab replays your code with the trial's log and compares the two runs. Read the **Replay** panel.";
const REPLAY_LOG = "Save, click **Restart** in the Driver Station, then **Replay the match**. The robot doesn't need to be enabled.";

const GREEN = "var(--good)";
const BLUE = "var(--accent)";
const MUTED = "var(--muted)";

const TRUE_RPM: PlotLine = {
	id: "truth:rpm",
	label: "True flywheel speed",
	color: MUTED,
	dashed: true,
};

export const CHALLENGES: Challenge[] = [
	{
		id: "example",
		number: 0,
		title: "Example: Hello, Logger",
		level: "Start here",
		files: [file("Challenge00Example")],
		summary:
			"Press A to add one to a counter, and write the counter to the log. This one's already written: run it and watch the **log** panel.",
		trial: "Taps A three times.",
		learn: ["Logger.recordOutput", "keys", "the log"],
		idea: [
			"Most robot code bugs happen on a field you can't stop, in a match you can't repeat. **AdvantageKit** is a logging framework that writes down what your robot's code saw and did, in every loop, so you can work out what happened afterwards.",
			"The simplest thing you can log is an **output**: a value your code worked out. You give it a **key**, a name with slashes in it like a folder path, and a value. `Logger.recordOutput(\"Hello/Presses\", presses)` files the value under `Hello/Presses`.",
			"This lesson's robot is simulated, but everything you write here is real command-based, real AdvantageKit code. The challenges also build on each other: later on you'll replay logs through your code, which is what makes AdvantageKit special.",
		],
		steps: [
			"Click **Start** in the Driver Station. The dot at the top of this tab turns green when the robot program is running.",
			"Click **Run trial**, then **Enable** the robot in **Teleop**. A scripted driver presses A three times, and the trial is graded.",
			"Watch `Hello/Presses` appear in the **log** panel and count up.",
			"Click **Free play**, enable the robot, and press **A** yourself (click the button, or press **K** on your keyboard).",
			"Open the **AdvantageScope** tab. Your value is under `AdvantageKit/RealOutputs/Hello/Presses`. Everything in the log panel is also there.",
		],
		code: [
			{
				code: 'Logger.recordOutput("Hello/Presses", presses);',
				text: "Writes the value to the log. The key can be anything you like: slashes make folders.",
			},
			{
				code: "controller.a().onTrue(Commands.runOnce(() -> { ... }));",
				text: "Runs some code once, each time A is pressed.",
			},
		],
		hints: [
			"Nothing to fix here. Read `Challenge00Example.java`, and try changing the key to see what happens.",
			"After any change to Java code, click **Restart** in the Driver Station so the program gets rebuilt.",
		],
		docs: [DOCS.what, DOCS.outputs, DOCS.scope],
		panels: {
			log: [{ key: "Hello/Presses", kind: "output" }],
			buttons: [{ name: "A", does: "Adds one to the counter" }],
		},
	},
	{
		id: "first-output",
		number: 1,
		title: "Your First Output",
		level: "Outputs",
		files: [file("Challenge01FirstOutput")],
		summary: "Write what the driver asked the flywheel to do to the log, every time it changes.",
		trial: "Taps A at 0.5 s to spin up to 3000 RPM, then taps B at 3 s to stop.",
		learn: ["Logger.recordOutput", "values persist", "units"],
		idea: [
			"A key keeps its value until something records a new one. So when you log something that changes rarely, like a setpoint, you only have to log it when it changes.",
			"In a robot log, a **setpoint** (what the code asked for) next to a **measurement** (what really happened) is the first thing you'll want to see for almost any mechanism.",
		],
		steps: [
			"In each of the two `runOnce` commands, add one line that records the setpoint at `Flywheel/SetpointRPM`.",
			RUN,
			"The dashed line on the plot is the flywheel's real speed. Your line should show what it was asked to do.",
		],
		code: [
			{
				code: 'Logger.recordOutput("Flywheel/SetpointRPM", 3000.0);',
				text: "Add `import org.littletonrobotics.junction.Logger;` at the top of the file too.",
			},
			{
				code: 'Logger.recordOutput("Flywheel/SetpointRPM", 3000.0, "rpm");',
				text: "For the bonus star: a third argument gives the value a unit. AdvantageScope uses it to label its graphs.",
			},
		],
		hints: [
			"Your line goes inside the `{ }` of the lambda, right where the TODO comment is.",
			"Use `3000.0`, not `3000`: the key should always hold the same type of value, and `0.0` is a `double` too.",
		],
		docs: [DOCS.outputs, DOCS.types],
		panels: {
			log: [{ key: "Flywheel/SetpointRPM", kind: "output" }],
			hide: ["Flywheel/VelocityRadPerSec", "Flywheel/AppliedVolts", "Flywheel/CurrentAmps", "Flywheel/CommandedVolts"],
			plots: [
				{
					title: "Flywheel",
					unit: "RPM",
					range: [0, 3200],
					lines: [{ id: "output:Flywheel/SetpointRPM", label: "Flywheel/SetpointRPM", color: BLUE }, TRUE_RPM],
				},
			],
			buttons: [
				{ name: "A", does: "Spin up to 3000 RPM" },
				{ name: "B", does: "Stop" },
			],
		},
	},
	{
		id: "every-loop",
		number: 2,
		title: "Log Every Loop",
		level: "Outputs",
		files: [file("Challenge02EveryLoop")],
		summary:
			"Log what the shooter is doing in every loop: its speed (a number), whether it's ready (a true/false), and its state (an enum).",
		trial: "Holds A from 0.5 s to 3.5 s.",
		learn: ["periodic()", "subtables", "booleans", "enums"],
		idea: [
			"A log is only a set of snapshots. If your code records a value once when a command starts, that's all the log will ever know, however much the real value changes afterwards.",
			"A subsystem's `periodic()` runs once per robot loop, whether or not any command is using it. That makes it the natural place to log anything that keeps changing.",
			"`Logger.recordOutput` takes more than numbers. Booleans show up as a true/false track in AdvantageScope. Enums are saved under their name, which makes state machines easy to read back.",
		],
		steps: [
			"Fill in `ShooterStatus.periodic()`, one key at a time. Restart and try it as often as you like.",
			"The three keys all start with `Shooter/`. Slashes put them in one folder in the log.",
			RUN,
		],
		code: [
			{
				code: 'Logger.recordOutput("Shooter/AtSpeed", flywheel.atSpeed());',
				text: "Booleans work just like numbers.",
			},
			{
				code: 'Logger.recordOutput("Shooter/State", ShooterState.READY);',
				text: "Enums are saved as text: `READY`.",
			},
			{
				code: `ShooterState state;
if (...) {
  state = ShooterState.IDLE;
} else if (...) {
  state = ShooterState.READY;
} else {
  state = ShooterState.SPINNING_UP;
}`,
				text: "One way to work out the state. Check for `IDLE` first.",
			},
		],
		hints: [
			"`flywheel.getRpm()` gives the speed, and `flywheel.getTargetRpm()` is 0 when nobody has asked it to spin.",
			"Try recording the speed in the `startEnd` command instead, and see what the plot does. That's why it goes in `periodic()`.",
		],
		docs: [DOCS.outputs, DOCS.types],
		panels: {
			log: [
				{ key: "Shooter/MeasuredRPM", kind: "output" },
				{ key: "Shooter/AtSpeed", kind: "output" },
				{ key: "Shooter/State", kind: "output" },
			],
			hide: ["Flywheel/"],
			plots: [
				{
					title: "Speed",
					unit: "RPM",
					range: [0, 3200],
					lines: [{ id: "output:Shooter/MeasuredRPM", label: "Shooter/MeasuredRPM", color: BLUE }, TRUE_RPM],
				},
				{
					title: "Ready",
					range: [0, 1],
					lines: [{ id: "output:Shooter/AtSpeed", label: "Shooter/AtSpeed", color: GREEN }],
				},
			],
			buttons: [{ name: "A", does: "Hold to spin up" }],
		},
	},
	{
		id: "poses",
		number: 3,
		title: "Log a Pose",
		level: "Outputs",
		files: [file("Challenge03Poses")],
		summary:
			"Log the robot's `Pose2d` and an array of waypoints. The field below only draws what you log, so it starts out empty.",
		trial: "Drives along the waypoints with the sticks.",
		learn: ["Pose2d", "arrays", "structured data"],
		idea: [
			"AdvantageKit logs WPILib's geometry classes as **structs**: compact binary values that carry their own description. AdvantageScope understands them, so you can drag a logged `Pose2d` onto a 2D field or a 3D view and watch the robot move.",
			"Arrays work too. Log every vision target, every path point or every module state as one key. It keeps a set of values that belong together in the same loop.",
		],
		steps: [
			"Fill in `PoseLogger.periodic()`. Each key takes one line.",
			RUN,
			"Now open the **AdvantageScope** tab, add a **2D Field** tab, and drag `Odometry/Robot` onto it.",
			"For the bonus star, keep a list of where the robot has been, and log it as `Odometry/Trail`.",
		],
		code: [
			{
				code: 'Logger.recordOutput("Odometry/Robot", drive.getPose());',
				text: "Objects like `Pose2d` log the same way as numbers.",
			},
			{
				code: 'Logger.recordOutput("Odometry/Waypoints", WAYPOINTS);',
				text: "So do arrays of them.",
			},
			{
				code: `private final List<Pose2d> trail = new ArrayList<>();
...
trail.add(drive.getPose());
Logger.recordOutput("Odometry/Trail", trail.toArray(new Pose2d[0]));`,
				text: "A growing list of poses, for the bonus star. A real robot would only add a pose now and then, to keep the log small.",
			},
		],
		hints: [
			"`WAYPOINTS` is already a `Pose2d[]`, so you can pass it straight to `recordOutput`.",
			"In free play, use **W A S D** to drive and the **arrow keys** to turn.",
		],
		docs: [DOCS.types, DOCS.outputs, DOCS.scope],
		panels: {
			log: [
				{ key: "Odometry/Robot", kind: "output" },
				{ key: "Odometry/Waypoints", kind: "output" },
			],
			hide: ["Drive/"],
			field: true,
		},
	},
	{
		id: "auto-log-output",
		number: 4,
		title: "Let the Annotation Do It",
		level: "Outputs",
		files: [file("Challenge04AutoLogOutput")],
		summary:
			"Log two fields and a method using `@AutoLogOutput`, without writing a single `Logger.recordOutput`.",
		trial: "Holds A from 0.5 s to 3 s, and taps X at 1 s and at 2 s.",
		learn: ["@AutoLogOutput", "automatic keys", "custom keys", "units"],
		idea: [
			"`@AutoLogOutput` goes on a field or a method. Once per loop, AdvantageKit reads it and logs the result, even if it's private. You can't forget to log it, and there's no extra code to read.",
			"Without a `key`, the key is the class name and the field name: `shotsRequested` in `ShooterTelemetry` becomes `ShooterTelemetry/ShotsRequested`. With one, you choose: `@AutoLogOutput(key = \"Shooter/Ready\")`.",
			"AdvantageKit finds annotations by looking through the objects your `Robot` holds, when the program starts. An object that gets made later, or that nothing keeps hold of, won't be found. The lab makes sure your challenge's objects are searched.",
		],
		steps: [
			"Add the annotation above each of the three members marked TODO.",
			RUN,
			"Check the **log** panel for the keys you expected, including the automatic one.",
		],
		code: [
			{
				code: "@AutoLogOutput\nprivate int shotsRequested = 0;",
				text: "Logged as `ShooterTelemetry/ShotsRequested`. Add `import org.littletonrobotics.junction.AutoLogOutput;`.",
			},
			{
				code: '@AutoLogOutput(key = "Shooter/TargetRPM", unit = "rpm")',
				text: "A key of your own, and a unit.",
			},
			{
				code: '@AutoLogOutput(key = "Shooter/Ready")\npublic boolean isReady() { ... }',
				text: "On a method, it logs whatever the method returns.",
			},
		],
		hints: [
			"The annotation goes on the line above the field or method, or right in front of it.",
			"Don't change what the fields hold, or add any `Logger` calls.",
		],
		docs: [DOCS.annotations, DOCS.types],
		panels: {
			log: [
				{ key: "ShooterTelemetry/ShotsRequested", kind: "output" },
				{ key: "Shooter/TargetRPM", kind: "output" },
				{ key: "Shooter/Ready", kind: "output" },
			],
			hide: ["Flywheel/"],
			buttons: [
				{ name: "A", does: "Hold to spin up" },
				{ name: "X", does: "Count a shot" },
			],
		},
	},
	{
		id: "process-inputs",
		number: 5,
		title: "Process Inputs",
		level: "Inputs and IO layers",
		files: [
			file("processinputs/Shooter"),
			file("processinputs/ShooterIO"),
			file("processinputs/ShooterIOSim"),
		],
		summary:
			"Finish the shooter's `periodic()`: read its sensors through the IO layer, and hand what it read to AdvantageKit. Until you do, it can't tell how fast it's going.",
		trial: "Holds A from 0.5 s to 3.5 s, then replays the log through your code.",
		learn: ["inputs", "updateInputs", "Logger.processInputs", "@AutoLog"],
		idea: [
			"So far you've logged outputs: what your code worked out. Here's the big idea of AdvantageKit. If you also log every **input**, everything your code *read*, you can run your code again later with the same inputs. It will do exactly what it did before, and you can look at anything in it. That's called **log replay**.",
			"To make that possible, a subsystem never talks to hardware itself. Its **IO layer** does. Once per loop, `io.updateInputs(inputs)` copies every sensor reading into one object, and `Logger.processInputs(\"Shooter\", inputs)` saves that object to the log. The rest of the subsystem only reads that object.",
			"During a replay, the same line `Logger.processInputs` does something different: it fills the object **from the log**. The code after it can't tell the difference, and that's the trick.",
		],
		steps: [
			"Read `ShooterIO.java`, then `ShooterIOSim.java`. You don't need to change them.",
			"In `Shooter.java`, fill in the two TODO lines at the top of `periodic()`.",
			RUN,
			"Look at the **log** panel. Your inputs are at the top level of the log, and your outputs have their own place. In AdvantageScope, outputs are under `RealOutputs`.",
			REPLAY_AFTER,
		],
		code: [
			{ code: "io.updateInputs(inputs);", text: "Asks the IO layer to read the sensors." },
			{
				code: 'Logger.processInputs("Shooter", inputs);',
				text: "Saves the inputs to the log, under `Shooter/`. In a replay, this fills them in from the log.",
			},
		],
		hints: [
			"Both lines go before anything that uses `inputs`, so that the rest of the loop sees this loop's readings.",
			"`getRpm()` reads `inputs.velocityRadPerSec`. That's why the flywheel ran away at full speed before: it read 0, so the code kept asking for more.",
		],
		docs: [DOCS.inputs, DOCS.io, DOCS.what],
		panels: {
			log: [
				{ key: "Shooter/VelocityRadPerSec", kind: "input" },
				{ key: "Shooter/AppliedVolts", kind: "input" },
				{ key: "Shooter/CommandedVolts", kind: "output" },
				{ key: "Shooter/MeasuredRPM", kind: "output" },
			],
			showKinds: true,
			plots: [
				{
					title: "Shooter",
					unit: "RPM",
					range: [0, 3200],
					lines: [{ id: "output:Shooter/MeasuredRPM", label: "Shooter/MeasuredRPM", color: BLUE }, TRUE_RPM],
				},
			],
			buttons: [{ name: "A", does: "Hold to spin up" }],
			replayKeys: ["Shooter/CommandedVolts"],
		},
	},
	{
		id: "write-an-io",
		number: 6,
		title: "Write an IO Layer",
		level: "Inputs and IO layers",
		files: [file("ioexercise/IntakeIO"), file("ioexercise/IntakeIOSim"), file("ioexercise/Intake")],
		summary:
			"Write the whole IO layer for the intake's rollers: the interface and its inputs, the simulated implementation, and the subsystem's `periodic()`.",
		trial: "A piece arrives at 0.3 s, and A is held from 1 s to 3 s to push it through.",
		learn: ["IO interfaces", "@AutoLog", "default methods", "implements"],
		idea: [
			"Every IO layer has the same shape. An **interface** lists what the subsystem can read (an `@AutoLog` inputs class) and what it can command (methods like `setVoltage`). Each **implementation** connects that to one kind of hardware.",
			"The interface's methods are `default` and empty. That means `new IntakeIO() {}` is a working IO layer with no hardware at all, which is exactly what replay needs.",
			"When you build, `@AutoLog` generates a class called `IntakeIOInputsAutoLogged`: your inputs, plus the code that saves them to a log and reads them back. Your subsystem holds one of those.",
		],
		steps: [
			"In `IntakeIO.java`, write the `@AutoLog` inputs class and the two default methods.",
			"In `IntakeIOSim.java`, override both methods using `Hardware.feeder`.",
			"In `Intake.java`, add the `inputs` field, then fill in `periodic()`.",
			RUN,
			"If you get stuck, the `subsystems/feeder/` folder has a finished one. Try without it first!",
			REPLAY_AFTER,
		],
		code: [
			{
				code: `@AutoLog
public static class IntakeIOInputs {
  public boolean hasPiece = false;
  public double appliedVolts = 0.0;
  public double currentAmps = 0.0;
}`,
				text: "Goes inside the interface. Every field needs a starting value.",
			},
			{
				code: "public default void setVoltage(double volts) {}",
				text: "A method that does nothing, unless an implementation overrides it.",
			},
			{
				code: "private final IntakeIOInputsAutoLogged inputs = new IntakeIOInputsAutoLogged();",
				text: "The generated class. It only exists after the inputs class has its `@AutoLog` and the code builds.",
			},
		],
		hints: [
			"`@AutoLog` needs `import org.littletonrobotics.junction.AutoLog;`, and `Hardware` needs `import frc.robot.lab.Hardware;`.",
			"If the editor says `IntakeIOInputsAutoLogged` doesn't exist, it may just need a build. Click **Restart** in the Driver Station and read the build output.",
			"`periodic()` needs three lines: `io.updateInputs(inputs)`, `Logger.processInputs(\"Intake\", inputs)`, and `io.setVoltage(volts)`.",
			"The replay needs an output to compare. Add `Logger.recordOutput(\"Intake/CommandedVolts\", volts)`.",
		],
		docs: [DOCS.io, DOCS.inputs, DOCS.uninitialized],
		panels: {
			log: [
				{ key: "Intake/HasPiece", kind: "input" },
				{ key: "Intake/AppliedVolts", kind: "input" },
				{ key: "Intake/CurrentAmps", kind: "input" },
			],
			showKinds: true,
			buttons: [{ name: "A", does: "Hold to run the rollers" }],
			piece: true,
		},
	},
	{
		id: "no-peeking",
		number: 7,
		title: "No Peeking",
		level: "Inputs and IO layers",
		files: [
			file("nopeeking/AutoFeeder"),
			file("nopeeking/AutoFeederIO"),
			file("nopeeking/AutoFeederIOSim"),
		],
		summary:
			"This robot works, and its replay is still wrong. Find where the subsystem reads the hardware itself, and move those reads to the IO layer.",
		trial: "A piece arrives at 0.2 s, and A is held from 0.5 s to 4 s. The feeder fires when the flywheel's ready.",
		learn: ["why inputs matter", "reading a failed replay", "fixing a leak"],
		idea: [
			"This is the rule that makes replay work: **every value your logic reads must come from the inputs object**. A subsystem that reads a sensor directly works perfectly on the robot. Nothing seems wrong until you replay a log, and then that reading isn't there.",
			"The lab's `Hardware` class behaves like real hardware: in a replay, it's gone. It gives zero and false to anything that asks. So code that peeks at it does something else in the replay, and the panel below shows you when and where.",
		],
		steps: [
			"Run the trial first, without changing anything. All the robot's behavior is right. Then read the **Replay** panel, and turn on `AutoFeeder/HasPiece` to see the two runs split.",
			"Find the two lines in `AutoFeeder.java` that use `Hardware`.",
			"The flywheel's speed is already in the inputs: use it. The beam break isn't, so add `hasPiece` to the inputs and read it in `AutoFeederIOSim.updateInputs`.",
			RUN,
		],
		code: [
			{
				code: "public boolean hasPiece = false;",
				text: "A new field in `AutoFeederIOInputs`. Its key will be `AutoFeeder/HasPiece`.",
			},
			{
				code: "inputs.hasPiece = Hardware.feeder.hasPiece();",
				text: "Goes in `AutoFeederIOSim.updateInputs`. An IO implementation is the right place for `Hardware`.",
			},
			{
				code: "boolean hasPiece = inputs.hasPiece;",
				text: "What the subsystem reads instead.",
			},
		],
		hints: [
			"Search `AutoFeeder.java` for the word `Hardware`. When you're done, its import should be gone too.",
			"The check named **The replay never touches the hardware** tells you the file and line of the first read it caught.",
		],
		docs: [DOCS.io, DOCS.nondeterministic, DOCS.replay],
		panels: {
			log: [
				{ key: "AutoFeeder/HasPiece", kind: "input" },
				{ key: "AutoFeeder/FlywheelVelocityRadPerSec", kind: "input" },
				{ key: "AutoFeeder/Feeding", kind: "output" },
				{ key: "AutoFeeder/Ready", kind: "output" },
			],
			hide: ["Flywheel/"],
			showKinds: true,
			buttons: [{ name: "A", does: "Hold to shoot" }],
			piece: true,
			replayKeys: ["AutoFeeder/Feeding", "AutoFeeder/HasPiece"],
		},
	},
	{
		id: "swap-io",
		number: 8,
		title: "One Robot, Three Modes",
		level: "Inputs and IO layers",
		files: [file("Challenge08SwapIO")],
		summary:
			"Pick the right IO layer for each mode: real hardware on the robot, a simulation here, and nothing at all in a replay.",
		trial: "Holds A from 0.5 s to 2.5 s, then replays the log through your code.",
		learn: ["Constants.currentMode", "swapping IO", "anonymous classes"],
		idea: [
			"An AdvantageKit project runs in three modes, set in `Constants.java`. **REAL** is on the roboRIO. **SIM** runs a physics simulation on your computer. **REPLAY** reads a log file. Only one line of each subsystem's setup needs to know which it is.",
			"In a replay, the IO layer has no job at all: `Logger.processInputs` fills the inputs from the log. So it gets an empty one, `new FlywheelIO() {}`. That also guarantees no replay can touch a real motor.",
			"Open `Robot.java` to see the other half: the same `switch` decides where the log goes. In **REPLAY**, it reads the old log with a `WPILOGReader`, and writes a new one with a `WPILOGWriter`. That's how the lab replays your code.",
		],
		steps: [
			"Change `createIO` so that it returns the right IO layer for each mode.",
			RUN,
			"Now read `Robot.java`. Find what happens in each mode.",
		],
		code: [
			{
				code: `return switch (mode) {
  case REAL -> new FlywheelIOSpark();
  case SIM -> new FlywheelIOSim();
  case REPLAY -> new FlywheelIO() {};
};`,
				text: "A `switch` that gives back a value. Java checks that you've handled every mode.",
			},
		],
		hints: [
			"`FlywheelIOSpark` needs `import frc.robot.subsystems.flywheel.FlywheelIOSpark;`.",
			"`new FlywheelIO() {}` makes an object of a new class with no name. It has no code of its own, so it uses the interface's empty default methods.",
		],
		docs: [DOCS.io, DOCS.replay],
		panels: {
			log: [{ key: "Flywheel/CommandedVolts", kind: "output" }],
			showKinds: true,
			buttons: [{ name: "A", does: "Hold to spin up" }],
			replayKeys: ["Flywheel/CommandedVolts"],
		},
	},
	{
		id: "hindsight",
		number: 9,
		title: "Log It in Hindsight",
		level: "Replay",
		files: [file("Challenge09Hindsight")],
		summary:
			"The practice match is over and you forgot to log something. Add it now, replay the match's log, and get the value for the whole match.",
		trial: "Replays `practice-match.wpilog`. No robot is needed: only your code and the log.",
		learn: ["replay", "adding outputs later", "RealOutputs and ReplayOutputs"],
		idea: [
			"This is what all those inputs were for. If you log every input, you can add an output **after the match**, replay the log, and get its value at every moment, just as if it had been there all along. With logging that only keeps what you asked for, that data would be gone.",
			"A replay writes a new log with both runs in it. **RealOutputs** holds what the robot logged at the match, and **ReplayOutputs** holds what your code logged this time. Your new key only exists in ReplayOutputs.",
		],
		steps: [
			"Click **Replay the match** first, without changing anything. Every output matches, because this is the same code that ran at the match.",
			"In `ShotAnalysis.periodic()`, record the difference between the flywheel's target and its speed.",
			REPLAY_LOG,
			"Turn on `Analysis/ErrorRPM` in the **Replay** panel. Where's the flywheel furthest from its target? Can you see where the shots happened?",
			"Then try the bonus: count the shots.",
		],
		code: [
			{
				code: 'Logger.recordOutput("Analysis/ErrorRPM", flywheel.getTargetRpm() - flywheel.getRpm());',
				text: "The target minus the speed.",
			},
			{
				code: `private boolean hadPiece = false;
private int shots = 0;
...
if (hadPiece && !feeder.hasPiece()) {
  shots++;
}
hadPiece = feeder.hasPiece();
Logger.recordOutput("Analysis/ShotCount", shots);`,
				text: "For the bonus: notice the moment a value changes, by remembering it from the last loop.",
			},
		],
		hints: [
			"The flywheel and feeder here read their sensors through IO layers, so they work in a replay. There's no robot to run, so you don't need to enable anything.",
			"The two runs can't affect the inputs. A replay feeds your code what the robot saw at the match. Whatever your new code does, the motors don't respond.",
		],
		docs: [DOCS.replay, DOCS.what, DOCS.comparison],
		panels: {
			log: [],
			showKinds: true,
			replayKeys: ["Analysis/ErrorRPM", "Analysis/ShotCount"],
		},
	},
	{
		id: "deterministic",
		number: 10,
		title: "Same Answer Every Time",
		level: "Replay",
		files: [file("Challenge10Deterministic")],
		summary:
			"This code never touches the hardware, and its replay still doesn't match. Find the two things in it that give a different answer each time, and fix them.",
		trial: "Holds A from 0.5 s to 3.5 s, then replays the log through your code.",
		learn: ["deterministic code", "Timer.getTimestamp", "seeded random"],
		idea: [
			"Replay assumes that the same inputs give the same outputs. Your code must be **deterministic**. Anything it gets from outside the log breaks that, even if it isn't hardware.",
			"The usual suspects are the clock and random numbers. A replay runs as fast as it can, so the wall clock races ahead of the log. AdvantageKit logs the time once per loop, and `Timer.getTimestamp()` gives you that logged time, so it's the same in both runs.",
			"You can still have randomness: give a `Random` a fixed **seed**, and it produces the same sequence every time. Others to watch for: `Timer.getFPGATimestamp()` (which always reads the real clock), and looping over a `HashMap` whose keys don't have a stable order.",
		],
		steps: [
			"Run the trial without changing anything, and look at `WarmUp/ElapsedSeconds` in the **Replay** panel.",
			"Replace the clock in `WarmUp` with `Timer.getTimestamp()`. It gives seconds, not milliseconds, so the arithmetic changes a little.",
			"Replace `Math.random()` with a `Random` object that has a fixed seed.",
			RUN,
		],
		code: [
			{
				code: "import edu.wpi.first.wpilibj.Timer;\n...\ndouble now = Timer.getTimestamp();",
				text: "The time of this loop, in seconds, from the log during a replay.",
			},
			{
				code: "private final Random random = new Random(1234);\n...\nrandom.nextDouble()",
				text: "Gives the same series of numbers every run, between 0 and 1, like `Math.random()`. Add `import java.util.Random;`.",
			},
		],
		hints: [
			"`startMillis` can become a `double startSeconds`, and `elapsed` no longer needs dividing by 1000.",
			"Try `Timer.getFPGATimestamp()` to see it fail too. It's the right clock for measuring how long code takes, but not for robot logic.",
		],
		docs: [DOCS.timestamps, DOCS.nondeterministic],
		panels: {
			log: [
				{ key: "WarmUp/ElapsedSeconds", kind: "output" },
				{ key: "WarmUp/WobbleRPM", kind: "output" },
				{ key: "WarmUp/TargetRPM", kind: "output" },
			],
			hide: ["Flywheel/"],
			plots: [
				{
					title: "Warm-up",
					unit: "RPM",
					range: [0, 3200],
					lines: [{ id: "output:WarmUp/TargetRPM", label: "WarmUp/TargetRPM", color: BLUE }, TRUE_RPM],
				},
			],
			buttons: [{ name: "A", does: "Hold to warm up" }],
			replayKeys: ["WarmUp/ElapsedSeconds", "WarmUp/WobbleRPM"],
		},
	},
	{
		id: "dashboard-inputs",
		number: 11,
		title: "Dashboard Inputs",
		level: "Replay",
		files: [file("Challenge11DashboardInputs")],
		summary:
			"Something typed on a dashboard is an input too. Replace the `SmartDashboard` read with a `LoggedNetworkNumber`, so that the replay gets the same value.",
		trial: "Holds A from 0.5 s to 4.5 s. The target changes to 4000 RPM at 2 s (and the preset to Far at 3.5 s).",
		learn: ["LoggedNetworkNumber", "LoggedDashboardChooser", "the /Tuning table"],
		idea: [
			"NetworkTables is a way into your program from outside. If you read it directly, the value never reaches the log, and a replay has no dashboard to ask, so it uses the default.",
			"AdvantageKit has logged versions of the usual things. `LoggedNetworkNumber`, `LoggedNetworkString` and `LoggedNetworkBoolean` are single values. `LoggedDashboardChooser` is a drop-in `SendableChooser`, the usual way to pick an auto. They read the network on a real run, log it, and read the log in a replay.",
			"`/Tuning/` is a good place for values you adjust while the robot runs. AdvantageScope can edit values in that table.",
		],
		steps: [
			"Run the trial first. The robot follows the dashboard fine, but the replay stays at 3000 RPM. See `TunedShooter/TargetRPM` in the **Replay** panel.",
			"Add the `LoggedNetworkNumber` field, and read `target` from it. Remove the `SmartDashboard` lines.",
			RUN,
			"Try the slider in free play, and watch the new input appear in the **log** panel.",
			"For the bonus star, add the `LoggedDashboardChooser`.",
		],
		code: [
			{
				code: `private final LoggedNetworkNumber targetRpm =
    new LoggedNetworkNumber("/Tuning/TargetRPM", 3000.0);
...
double target = targetRpm.get();`,
				text: "Import it from `org.littletonrobotics.junction.networktables`.",
			},
			{
				code: `private final LoggedDashboardChooser<Double> preset =
    new LoggedDashboardChooser<>("ShotPreset");
...
preset.addDefaultOption("Near", 0.0);
preset.addOption("Far", 1000.0);
...
Double extra = preset.get();
double target = targetRpm.get() + (extra == null ? 0.0 : extra);`,
				text: "A chooser for the bonus star. It also lives in `networktables`. It has nothing to give until its first loop, so `get()` can return `null`.",
			},
		],
		hints: [
			"The field goes in `TunedShooter`, where the TODO comment is.",
			"Your dashboard inputs are logged under `NetworkInputs/`: look for them in the **log** panel.",
		],
		docs: [DOCS.dashboard, DOCS.nondeterministic],
		panels: {
			log: [
				{ key: "NetworkInputs/Tuning/TargetRPM", kind: "dashboard" },
				{ key: "TunedShooter/TargetRPM", kind: "output" },
			],
			hide: ["Flywheel/"],
			showKinds: true,
			plots: [
				{
					title: "Target",
					unit: "RPM",
					range: [0, 5200],
					lines: [{ id: "output:TunedShooter/TargetRPM", label: "TunedShooter/TargetRPM", color: BLUE }, TRUE_RPM],
				},
			],
			buttons: [{ name: "A", does: "Hold to spin up" }],
			tuning: true,
			replayKeys: ["TunedShooter/TargetRPM"],
		},
	},
	{
		id: "log-commands",
		number: 12,
		title: "Log the Scheduler",
		level: "Replay",
		files: [file("Challenge12LogCommands")],
		summary:
			"Keep a key for every command that's true while the command runs. The timeline below draws your keys, including what happens when one command interrupts another.",
		trial: "Holds A for a while, taps X, then holds A again and interrupts it with B.",
		learn: ["onCommandInitialize", "onCommandFinish", "onCommandInterrupt", "lambdas"],
		idea: [
			"In a command-based robot, \"why did it do that?\" usually means \"which command was running?\". The `CommandScheduler` can call your code every time a command starts, finishes or gets interrupted, so you can log that alongside everything else.",
			"There are three ways for a command to stop being scheduled, and they're easy to miss. A command **finishes** when it says it's done. It's **interrupted** if another command takes its subsystem, or if it's cancelled. Letting go of a `whileTrue` button is a cancel too!",
			"Teams often log a key per command, using its name. Give your commands names with `.withName(\"...\")`, or they'll be called things like `InstantCommand`.",
		],
		steps: [
			"Fill in `logCommands()`. Start with `onCommandInitialize` and `onCommandFinish`, and run the trial. What does the timeline show for `SpinUp` when A is let go?",
			"Now handle the third case.",
			RUN,
			"Watch out if you add each command's `hashCode()` to its key, as some teams do: it changes from run to run, so it would fail the replay.",
		],
		code: [
			{
				code: `CommandScheduler.getInstance()
    .onCommandInitialize(
        command -> Logger.recordOutput("CommandsAll/" + command.getName(), true));`,
				text: "The part after the arrow runs each time a command starts.",
			},
			{
				code: "onCommandFinish(command -> ...)\nonCommandInterrupt(command -> ...)",
				text: "The other two. Each also takes a lambda, and there's another form of `onCommandInterrupt` that tells you what did the interrupting.",
			},
		],
		hints: [
			"`CommandScheduler` needs `import edu.wpi.first.wpilibj2.command.CommandScheduler;`.",
			"The lab clears these callbacks when you run another challenge. On a real robot, you'd register them once, in `RobotContainer` or `Robot`.",
		],
		docs: [DOCS.scheduler, DOCS.outputs],
		panels: {
			log: [],
			hide: ["Flywheel/", "Feeder/"],
			commands: true,
			buttons: [
				{ name: "A", does: "Hold: SpinUp" },
				{ name: "B", does: "Hold: Eject" },
				{ name: "X", does: "Feed for half a second" },
			],
			piece: true,
			replayKeys: ["CommandsAll/SpinUp"],
		},
	},
	{
		id: "fast-sensors",
		number: 13,
		title: "Faster Than the Loop",
		level: "Advanced",
		files: [file("fastsensors/GyroIOSim"), file("fastsensors/Heading"), file("fastsensors/GyroIO")],
		summary:
			"The gyro takes five samples for every robot loop, and a hit on the robot can be over between loops. Get every sample through the inputs, and add them all up.",
		trial: "The robot turns slowly, and takes three short, sharp hits. Nothing presses any buttons.",
		learn: ["array inputs", "sampling", "threads belong in IO"],
		idea: [
			"Your code's loop runs at 50 Hz, but sensors can be much faster. If you only look once per loop, anything between looks never happened. Teams run their swerve odometry at 250 Hz for this very reason.",
			"Inputs don't have to be single values. An **array** input can hold everything that arrived since the last loop.",
			"On a real robot, a background thread collects the samples. Threads can't be replayed, because their timing is different every time, so the rule is: **threads stay inside the IO implementation**. They hand over what they've collected as an input, and all your other code stays on one thread.",
		],
		steps: [
			"Run the trial first. See how far the log gets from the true heading, on the plot.",
			"In `GyroIOSim.updateInputs`, fill in both arrays. In the log panel, the arrays should now hold five samples.",
			"In `Heading.periodic()`, replace the TODO's code with a loop that adds up every sample.",
			RUN,
		],
		code: [
			{
				code: "inputs.yawRateRadPerSec = Hardware.gyro.getYawRateSamples();\ninputs.sampleTimestamps = Hardware.gyro.getSampleTimestamps();",
				text: "Every sample since the last loop. Add `import frc.robot.lab.Hardware;`.",
			},
			{
				code: "for (double rate : inputs.yawRateRadPerSec) {\n  yawRad += rate * SAMPLE_PERIOD;\n}",
				text: "Each sample covers 4 ms, not the whole 20 ms loop.",
			},
		],
		hints: [
			"Array inputs need a starting value, like `new double[] {}`. A `null` would crash the logger. Look at `GyroIO.java`.",
			"Multiplying by the wrong time step is an easy mistake here: with five samples, each one lasts `0.004` seconds.",
		],
		docs: [DOCS.odometry, DOCS.threads, DOCS.uninitialized],
		panels: {
			log: [
				{ key: "Gyro/YawRateRadPerSec", kind: "input" },
				{ key: "Gyro/SampleTimestamps", kind: "input" },
				{ key: "Heading/YawDegrees", kind: "output" },
			],
			showKinds: true,
			plots: [
				{
					title: "Heading",
					unit: "deg",
					lines: [
						{ id: "output:Heading/YawDegrees", label: "Heading/YawDegrees", color: BLUE },
						{ id: "truth:yawDeg", label: "True heading", color: MUTED, dashed: true },
					],
				},
			],
			replayKeys: ["Heading/YawDegrees"],
		},
	},
	{
		id: "from-scratch",
		number: 14,
		title: "From Scratch",
		level: "Put it together",
		files: [
			file("Challenge14FromScratch"),
			file("fromscratch/PivotIO"),
			file("fromscratch/PivotIOSim"),
			file("fromscratch/Pivot"),
		],
		summary:
			"Build a complete AdvantageKit subsystem by yourself: a pivoting arm with an IO layer and commands, whose replay comes out perfect.",
		trial: "Taps A at 0.5 s to go to 90 degrees, and taps B at 3 s to go back to 0 degrees.",
		learn: ["everything so far"],
		idea: [
			"This is the whole pattern, from an empty folder. If you've got this, you can add AdvantageKit to any subsystem on a real robot: only the IO implementation would change.",
			"Build it in the usual order: the IO interface and its inputs, the sim implementation, the subsystem, and then the commands.",
		],
		steps: [
			"Read `fromscratch/package-info.java`, which says what hardware the pivot has.",
			"Write `PivotIO.java` (with fields `positionRad`, `velocityRadPerSec` and `appliedVolts`), and `PivotIOSim.java`.",
			"Write `Pivot.java`. It should go to a setpoint, in `periodic()`, and log it as `Pivot/SetpointDegrees`.",
			"In `Challenge14FromScratch.java`, make the subsystem with the right IO for the mode, and bind A and B.",
			RUN,
			"For the bonus star, add a method called `atSetpoint()`, and log it at `Pivot/AtSetpoint` with `@AutoLogOutput`.",
		],
		code: [
			{
				code: "double errorRad = Units.degreesToRadians(setpointDegrees) - inputs.positionRad;\nio.setVoltage(MathUtil.clamp(12.0 * errorRad, -12.0, 12.0));",
				text: "A simple controller for `periodic()`.",
			},
			{
				code: "new Pivot(\n    Constants.currentMode == Constants.Mode.REPLAY ? new PivotIO() {} : new PivotIOSim());",
				text: "Choosing the IO layer in the challenge, like the earlier challenges do.",
			},
		],
		hints: [
			"Look back at `Challenge06` and your `ioexercise` code for the shape of each file.",
			"`pivot.runOnce(() -> pivot.setSetpointDegrees(90.0))` is all a button needs.",
			"Remember your subsystem's `periodic()` needs to update and process its inputs, before anything else.",
		],
		docs: [DOCS.io, DOCS.outputs, DOCS.annotations],
		panels: {
			log: [
				{ key: "Pivot/PositionRad", kind: "input" },
				{ key: "Pivot/AppliedVolts", kind: "input" },
				{ key: "Pivot/SetpointDegrees", kind: "output" },
			],
			showKinds: true,
			plots: [
				{
					title: "Pivot",
					unit: "deg",
					range: [0, 100],
					lines: [
						{ id: "output:Pivot/SetpointDegrees", label: "Pivot/SetpointDegrees", color: BLUE },
						{ id: "truth:pivotDeg", label: "True position", color: MUTED, dashed: true },
					],
				},
			],
			buttons: [
				{ name: "A", does: "Go to 90 degrees" },
				{ name: "B", does: "Go to 0 degrees" },
			],
			replayKeys: ["Pivot/SetpointDegrees"],
		},
	},
	{
		id: "mystery",
		number: 15,
		title: "The Qual 17 Mystery",
		level: "Put it together",
		files: [file("mystery/Shooter")],
		summary:
			"In match 17, one of the robot's three shots missed badly. All you have is the log and the code. Work out what happened, fix the bug, and prove your fix by replaying the match.",
		trial: "Replays `qual-17.wpilog` through your code. The robot isn't needed, or even in the building.",
		learn: ["debugging from a log", "hindsight outputs", "proving a fix"],
		idea: [
			"This is what AdvantageKit is really for. The drive team did everything right, the bug only shows up once in a while, and the robot's been taken apart. Teams have found bugs this way that they never could have reproduced in the pit.",
			"Work like a detective. Look at what happened, form a theory, then log whatever would prove it. Each replay takes a few seconds, and you can add as many outputs as you like.",
			"A replay can't show you a different match: its inputs are what really happened. If your fix would have changed how the robot moved, the log's inputs still show the old motion. But you can see exactly what your code would have *decided* at each moment, and for a bug like this, that's the proof you need.",
		],
		steps: [
			"Click **Replay the match** with no changes. Turn on `Shooter/TargetRPM`, `Shooter/Feeding` and the inputs. Which shot looks different?",
			"Read `Shooter.java`. What has to be true before it feeds a piece?",
			"Log something that would prove your theory, in `periodic()`. How far is the flywheel from its target at the moment each shot starts?",
			"Find the bug and fix it, and replay until your code only starts feeding once the flywheel is ready. Now the replay *should* differ from the match!",
		],
		code: [
			{
				code: 'Logger.recordOutput("Shooter/ErrorRPM", targetRpm - rpm);',
				text: "This is worth the bonus star, and it's a big clue.",
			},
		],
		hints: [
			"Look at the moments `Shooter/Feeding` turns on, and what the speed is then. To compare RPM with rad/s, multiply rad/s by about 9.55.",
			"The shot that missed is the one where the driver chose the far target, and fired right away.",
			"How close is close enough? Look carefully at every constant at the top of `Shooter.java`.",
		],
		docs: [DOCS.cases, DOCS.replay, DOCS.what],
		panels: {
			log: [],
			showKinds: true,
			replayKeys: ["Shooter/TargetRPM", "Shooter/Feeding"],
		},
	},
];

export function findChallenge(id: string | undefined): Challenge | undefined {
	return CHALLENGES.find((challenge) => challenge.id === id);
}

export function optionLabel(challenge: Challenge): string {
	return challenge.number > 0 ? `${challenge.number}. ${challenge.title}` : challenge.title;
}
