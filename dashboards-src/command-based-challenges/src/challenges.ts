/**
 * Every challenge the dashboard offers. The `id`s must match the ones in the
 * lesson's `Challenges.java`, and each file must exist in the module. The
 * robot program owns what each trial does and how it is graded; this file
 * holds what the student reads.
 *
 * Text fields may use `backticks` for inline code and **stars** for bold.
 */

export type Level =
	| "Start here"
	| "Building commands"
	| "Triggers and interrupts"
	| "Your own subsystems and commands"
	| "Autonomous"
	| "Capstone";

export type CodeRef = { code: string; text: string };
export type DocLink = { title: string; url: string };

export type Challenge = {
	id: string;
	number: number;
	title: string;
	level: Level;
	/** Project-relative files the student edits, the main one first. */
	files: string[];
	/** The Driver Station mode the trial runs in. */
	mode: "teleop" | "auto";
	/** One or two sentences: what the trial asks for. */
	summary: string;
	/** What the trial's scripted driver does, so the student knows what's tested. */
	trial: string;
	/** Ideas this challenge practises. */
	learn: string[];
	/** The command-based idea behind it, a paragraph each. */
	idea: string[];
	steps: string[];
	code: CodeRef[];
	/** Revealed one at a time. */
	hints: string[];
	docs: DocLink[];
};

const CHALLENGE_DIR = "src/main/java/frc/robot/challenges";
const challengeFile = (name: string) => `${CHALLENGE_DIR}/${name}.java`;

const WPILIB = "https://docs.wpilib.org/en/stable/docs/software";
const doc = (title: string, page: string): DocLink => ({
	title: `WPILib: ${title}`,
	url: `${WPILIB}/${page}.html`,
});

const DOCS = {
	whatIs: doc("What Is Command-Based Programming?", "commandbased/what-is-command-based"),
	commands: doc("Commands", "commandbased/commands"),
	compositions: doc("Command Compositions", "commandbased/command-compositions"),
	subsystems: doc("Subsystems", "commandbased/subsystems"),
	triggers: doc("Binding Commands to Triggers", "commandbased/binding-commands-to-triggers"),
	scheduler: doc("The Command Scheduler", "commandbased/command-scheduler"),
	organizing: doc("Organizing Command-Based Robot Projects", "commandbased/organizing-command-based"),
	structuring: doc("Structuring a Command-Based Robot Project", "commandbased/structuring-command-based-project"),
	pid: doc("PID Control in WPILib", "advanced-controls/controllers/pidcontroller"),
	alliance: doc("Alliance Color", "basic-programming/alliancecolor"),
	lambdas: doc("Treating Functions as Data (lambdas)", "basic-programming/functions-as-data"),
	ppBuildAuto: { title: "PathPlanner: Build an Auto", url: "https://pathplanner.dev/pplib-build-an-auto.html" },
	ppNamed: { title: "PathPlanner: Named Commands", url: "https://pathplanner.dev/pplib-named-commands.html" },
	ppGui: {
		title: "PathPlanner: Editing Paths and Autos",
		url: "https://pathplanner.dev/gui-editing-paths-and-autos.html",
	},
};

const ENABLE_TELEOP =
	"Save, click **Restart** in the Driver Station, then **Run trial**. The trial starts when you **Enable** the robot in **Teleop**.";
const ENABLE_AUTO =
	"Save, click **Restart** in the Driver Station, then **Run trial**. Set the Driver Station to **Autonomous** and click **Enable**.";

const SHOOT_CODE = `Commands.sequence(
    shooter.runOnce(() -> shooter.setRPM(Shooter.SHOT_RPM)),
    Commands.waitUntil(shooter::atSpeed),
    indexer.runEnd(() -> indexer.setSpeed(Indexer.FEED_SPEED), indexer::stop)
        .until(() -> !indexer.hasPiece()),
    Commands.waitSeconds(0.2))
  .finallyDo(() -> { shooter.stop(); indexer.stop(); })`;

const INTAKE_CODE = `Commands.runEnd(
    () -> {
      intake.deploy();
      intake.setRollers(Intake.INTAKE_SPEED);
      indexer.setSpeed(Indexer.LOAD_SPEED);
    },
    () -> {
      intake.stopRollers();
      intake.retract();
      indexer.stop();
    },
    intake, indexer)
  .until(indexer::hasPiece)`;

export const CHALLENGES: Challenge[] = [
	{
		id: "example",
		number: 0,
		title: "Example: Hello, Commands",
		level: "Start here",
		files: [challengeFile("Challenge00Example")],
		mode: "teleop",
		summary:
			"Hold X to spin the intake rollers, and press Y to print a message. This one is already written: run it and watch the **command timeline**.",
		trial: "Holds X from 0.5 s to 2 s, then taps Y.",
		learn: ["subsystems", "commands", "triggers", "requirements"],
		idea: [
			"In a **command-based** robot, the robot's parts are **subsystems** (the drive, the intake, the shooter...) and everything the robot does is a **command**: a job with a start, a middle and an end.",
			"Every 20 ms, the **command scheduler** checks the **triggers** (buttons, sensors...), starts the commands bound to them, runs every command that's running, and ends the ones that are done.",
			"Each command **requires** the subsystems it uses. Two commands that need the same subsystem can't run at the same time: starting one interrupts the other. That way, a motor never gets two sets of orders.",
		],
		steps: [
			"Click **Start** in the Driver Station. The dot at the top of this tab turns green when the robot program is running.",
			"Click **Run trial**, then **Enable** the robot in **Teleop**. The trial's scripted driver holds X.",
			"Watch the **Command timeline**: a bar appears in the Intake lane while X is held, and X lights up on the controller. Trials press the buttons for you, so you never need a controller for them.",
			"Click **Free play** and try it yourself. No gamepad needed: click this tab and hold **J** (the X button's key), or hold X on the controller drawn here. Every button's key is drawn next to it.",
			"Open `Challenge00Example.java` and read how it's built.",
		],
		code: [
			{
				code: "intake.runEnd(whileRunning, whenEnded)",
				text: "A command that requires the intake. It runs `whileRunning` every loop, and `whenEnded` once when it ends.",
			},
			{
				code: "controller.x().whileTrue(command)",
				text: "Starts the command when X is pressed, and cancels it when X is let go.",
			},
			{
				code: '.withName("Spin rollers")',
				text: "Names the command, so you can tell commands apart on the timeline.",
			},
			{
				code: 'Commands.print("Hello!")',
				text: "A command that prints a message to the robot program's console.",
			},
		],
		hints: ["There's nothing to write: the example passes as it is. Try changing it, though! What happens with `onTrue` instead of `whileTrue`?"],
		docs: [DOCS.whatIs, DOCS.commands, DOCS.scheduler],
	},
	{
		id: "spin-up",
		number: 1,
		title: "Spin Up",
		level: "Start here",
		files: [challengeFile("Challenge01SpinUp")],
		mode: "teleop",
		summary: "Press A to spin up the shooter, and B to stop it.",
		trial: "Taps A at 0.5 s and B at 3 s.",
		learn: ["runOnce", "onTrue", "requirements", "lambdas"],
		idea: [
			"`onTrue` starts a command once, when the button is **pressed** (when it goes from false to true). Holding the button doesn't start it again, and letting go doesn't stop it.",
			"`shooter.runOnce(action)` makes a command that runs `action` once and finishes straight away. That's enough here: the shooter's motor controller keeps the wheel at the speed you asked for until something asks for another.",
			"Because it's made from `shooter`, the command **requires** the shooter. `Commands.runOnce(action)` would make one that requires nothing, and then the scheduler couldn't stop two commands from fighting over the shooter.",
		],
		steps: [
			"In `configureBindings`, bind A with `controller.a().onTrue(...)` to a command that calls `shooter.setRPM(Shooter.SHOT_RPM)`.",
			"Bind B to a command that calls `shooter.stop()`.",
			ENABLE_TELEOP,
			"Watch the Shooter lane on the timeline: each press makes a tiny bar, because a `runOnce` command finishes the moment it starts.",
		],
		code: [
			{
				code: "shooter.runOnce(() -> shooter.setRPM(Shooter.SHOT_RPM))",
				text: "A command that asks for 3000 RPM, once. `() -> ...` is a **lambda**: a little piece of code the command keeps and runs later.",
			},
			{
				code: "shooter.runOnce(shooter::stop)",
				text: "`shooter::stop` is a **method reference**, a shorter way to write `() -> shooter.stop()`.",
			},
			{ code: "controller.a().onTrue(command)", text: "Runs the command when A is pressed." },
			{ code: '.withName("Spin up")', text: "Names the command (for the bonus star)." },
		],
		hints: [
			"You need one line for A and one for B.",
			"`controller.a().onTrue(shooter.runOnce(() -> shooter.setRPM(Shooter.SHOT_RPM)));`",
		],
		docs: [DOCS.commands, DOCS.triggers, DOCS.lambdas],
	},
	{
		id: "hold-to-intake",
		number: 2,
		title: "Hold to Intake",
		level: "Start here",
		files: [challengeFile("Challenge02HoldToIntake")],
		mode: "teleop",
		summary: "While LB is held, swing the intake out and spin its rollers. Let go to stop and put it away.",
		trial: "Holds LB from 0.5 s to 2.5 s, then again from 3.5 s to 4 s.",
		learn: ["whileTrue", "startEnd", "cleaning up in end"],
		idea: [
			"`whileTrue` starts a command when the button is pressed and **cancels** it when the button is let go.",
			"A cancelled command still gets to clean up: its **end** always runs. `intake.startEnd(onStart, onEnd)` runs `onStart` once when the command starts, and `onEnd` once when it ends, however it ends.",
			"If a command doesn't put things back when it ends, nothing will: motors keep doing the last thing they were told.",
		],
		steps: [
			"Make the command with `intake.startEnd(...)`. When it starts: deploy the intake and run the rollers at `Intake.INTAKE_SPEED`. When it ends: stop the rollers and retract the intake.",
			"Bind it to `controller.leftBumper()` with `whileTrue`.",
			ENABLE_TELEOP,
			"In free play, hold LB (key **E**) and click **+ Piece**: the intake grabs it. (Getting it all the way in is challenge 5.)",
		],
		code: [
			{
				code: "intake.startEnd(\n    () -> { intake.deploy(); intake.setRollers(Intake.INTAKE_SPEED); },\n    () -> { intake.stopRollers(); intake.retract(); })",
				text: "Curly braces let a lambda do more than one thing.",
			},
			{ code: "controller.leftBumper().whileTrue(command)", text: "Runs the command while LB is held." },
		],
		hints: [
			"With `onTrue` instead of `whileTrue`, the command would never end, because nothing cancels it.",
			"Put the command in a variable first: `Command runIntake = intake.startEnd(...);` then `controller.leftBumper().whileTrue(runIntake);`",
		],
		docs: [DOCS.commands, DOCS.triggers],
	},
	{
		id: "toggle",
		number: 3,
		title: "Toggle",
		level: "Start here",
		files: [challengeFile("Challenge03Toggle")],
		mode: "teleop",
		summary: "Press Y to turn the shooter on, and Y again to turn it off. Bonus: X always turns it off.",
		trial: "Taps Y at 0.5 s, 2.5 s and 4 s, then X at 5.2 s.",
		learn: ["toggleOnTrue", "interrupting a command"],
		idea: [
			"`toggleOnTrue` starts the command on one press and cancels it on the next.",
			"Make the command with `shooter.startEnd(...)`: spin up when it starts, stop when it ends. Then cancelling it stops the wheel.",
			"Bonus: a command bound to X that requires the shooter **interrupts** the toggled command, and its end stops the wheel. Requirements decide who's in charge of a subsystem.",
		],
		steps: [
			"Make a command that spins the shooter up when it starts and stops it when it ends.",
			"Bind it to `controller.y()` with `toggleOnTrue`.",
			"Bonus: bind X to `shooter.runOnce(shooter::stop)`.",
			ENABLE_TELEOP,
		],
		code: [
			{
				code: "shooter.startEnd(() -> shooter.setRPM(Shooter.SHOT_RPM), shooter::stop)",
				text: "Spins up when it starts, stops when it ends.",
			},
			{ code: "controller.y().toggleOnTrue(command)", text: "Each press of Y starts or cancels the command." },
		],
		hints: [
			"Look at the timeline after pressing X: the toggled command's bar ends with an interrupt mark.",
			"`controller.y().toggleOnTrue(shooter.startEnd(() -> shooter.setRPM(Shooter.SHOT_RPM), shooter::stop));`",
		],
		docs: [DOCS.triggers],
	},
	{
		id: "drive-with-sticks",
		number: 4,
		title: "Drive with the Sticks",
		level: "Building commands",
		files: [challengeFile("Challenge04DriveWithSticks")],
		mode: "teleop",
		summary: "Give the drivetrain a default command that drives the robot with the sticks, field-relative.",
		trial:
			"Pushes the left stick up, then left, then the right stick right, and lets go. Bonus: pushes up again after the robot has turned, then lets the left stick drift slightly.",
		learn: ["default commands", "run", "reading inputs every loop", "deadband", "field-relative"],
		idea: [
			"A **default command** runs whenever nothing else requires its subsystem. A drivetrain's default command is nearly always \"drive with the sticks\". When another command needs the drive (an auto-aim, say), it takes over, and the default command comes back when it ends.",
			"`drive.run(action)` makes a command that runs `action` **every loop** until it's cancelled. Read the sticks *inside* the lambda. Code outside it runs only once, when the challenge starts, so it would read the sticks only once!",
			"Sticks never rest at exactly 0. A **deadband** treats tiny values as 0, so the robot doesn't creep: `MathUtil.applyDeadband(value, 0.1)`.",
			"**Field-relative** driving means pushing up always drives away from the driver, whichever way the robot is facing. That's what drivers expect.",
		],
		steps: [
			"Make a command with `drive.run(...)` that calls `drive.driveFieldRelative(xSpeed, ySpeed, rotationSpeed)`, reading the sticks inside the lambda.",
			"Make it the drive's default command with `drive.setDefaultCommand(...)`.",
			ENABLE_TELEOP,
			"In free play, drive around the field: click this tab, then **W A S D** move and the **arrow keys** turn (hold **Shift** to go slower). The Drive lane on the timeline shows your default command running.",
		],
		code: [
			{ code: "drive.setDefaultCommand(command)", text: "The command the drive runs when nothing else needs it." },
			{
				code: "drive.run(() -> drive.driveFieldRelative(x, y, rotation))",
				text: "Drives every loop. Speeds are in m/s and rad/s; +X is away from your driver station, +Y is left, and positive rotation turns left.",
			},
			{
				code: "controller.getLeftY()",
				text: "The left stick, from -1 to 1. **Up is negative**, and so is left on `getLeftX()`.",
			},
			{ code: "Drive.MAX_SPEED  Drive.MAX_ANGULAR_SPEED", text: "Multiply stick values by these to get speeds." },
			{ code: "MathUtil.applyDeadband(controller.getLeftY(), 0.1)", text: "Treats anything between -0.1 and 0.1 as 0." },
		],
		hints: [
			"Forward speed: `-controller.getLeftY() * Drive.MAX_SPEED`. Sideways: `-controller.getLeftX() * Drive.MAX_SPEED`.",
			"Turning: `-controller.getRightX() * Drive.MAX_ANGULAR_SPEED`. Pushing right should turn clockwise, which is a negative rotation.",
			"If the robot doesn't move at all, check that you call `controller.getLeftY()` inside the `() -> ...`, not before it.",
		],
		docs: [DOCS.subsystems, DOCS.commands, DOCS.lambdas],
	},
	{
		id: "intake-until-loaded",
		number: 5,
		title: "Intake Until Loaded",
		level: "Building commands",
		files: [challengeFile("Challenge05IntakeUntilLoaded")],
		mode: "teleop",
		summary:
			"Hold LB to pick up a piece: run the intake and the indexer until the beam break sees the piece, then stop by yourself, even if LB is still held.",
		trial:
			"Holds LB from 0.5 s to 4 s; a human player rolls a piece into the intake at 1.2 s. Bonus: presses LB again with the piece already loaded.",
		learn: ["two requirements", "until", "unless", "sensors"],
		idea: [
			"`Commands.runEnd(whileRunning, whenEnded, intake, indexer)` makes a command that requires **both** subsystems. The intake rollers grab the piece, and the indexer pulls it in to the beam break.",
			"`.until(condition)` makes a command end as soon as the condition is true. Here that's `indexer::hasPiece`. It ends by itself, and `whileTrue` won't start it again until LB is pressed again.",
			"Watch the side view: if the indexer keeps running once the piece reaches the beam break, it pushes the piece on into the shooter, which isn't spinning. The piece dribbles out of the robot.",
			"Bonus: `.unless(indexer::hasPiece)` stops the command from even starting when there's already a piece.",
		],
		steps: [
			"Make the command with `Commands.runEnd(...)`, requiring `intake` and `indexer`.",
			"Add `.until(indexer::hasPiece)`.",
			"Bind it to LB with `whileTrue`.",
			ENABLE_TELEOP,
		],
		code: [
			{ code: INTAKE_CODE, text: "Picks up a piece and stops at the beam break." },
			{ code: "indexer.hasPiece()", text: "The beam break: true when a piece is in front of it." },
			{ code: ".unless(indexer::hasPiece)", text: "Doesn't start the command at all if this is true when it's scheduled." },
		],
		hints: [
			"Run the indexer at `Indexer.LOAD_SPEED`. The intake rollers alone can't get the piece to the beam break.",
			"The command's end should stop the rollers, stop the indexer and retract the intake.",
		],
		docs: [DOCS.compositions, DOCS.commands],
	},
	{
		id: "shoot",
		number: 6,
		title: "Shoot!",
		level: "Building commands",
		files: [challengeFile("Challenge06Shoot")],
		mode: "teleop",
		summary:
			"Pull RT once to shoot the piece: spin up, wait until the shooter is at speed, feed the piece in, then stop.",
		trial: "The robot starts at the shooting spot holding a piece. Pulls RT at 0.5 s.",
		learn: ["sequence", "waitUntil", "waitSeconds", "andThen"],
		idea: [
			"`Commands.sequence(a, b, c)` runs commands one after another: each one starts when the one before it finishes. `a.andThen(b)` is another way to write it.",
			"`Commands.waitUntil(shooter::atSpeed)` does nothing until its condition is true, then finishes. It's how a sequence waits for something to happen.",
			"A piece flies about 1 m for every 1000 RPM. Feed it in while the wheel is still spinning up and it falls short, or just dribbles out.",
			"A sequence requires every subsystem that any of its steps requires, for the whole time it runs.",
		],
		steps: [
			"Start the shooter with `shooter.runOnce(...)`.",
			"Wait with `Commands.waitUntil(shooter::atSpeed)`.",
			"Feed: run the indexer at `Indexer.FEED_SPEED` until `!indexer.hasPiece()`.",
			"Wait 0.2 s for the piece to clear the wheels, then stop the shooter.",
			"Put the steps in a `Commands.sequence(...)` and bind it to `controller.rightTrigger()` with `onTrue`.",
			ENABLE_TELEOP,
		],
		code: [
			{ code: "Commands.sequence(first, second, third)", text: "One after another." },
			{ code: "Commands.waitUntil(shooter::atSpeed)", text: "Finishes when the shooter is within 150 RPM of its target." },
			{
				code: "indexer.runEnd(() -> indexer.setSpeed(Indexer.FEED_SPEED), indexer::stop)\n    .until(() -> !indexer.hasPiece())",
				text: "Feeds until the piece has gone. `!` means \"not\".",
			},
			{ code: "Commands.waitSeconds(0.2)", text: "Waits, then finishes." },
			{ code: "controller.rightTrigger()", text: "True when RT is pulled more than halfway." },
		],
		hints: [
			"Your sequence has five steps: spin up, wait for speed, feed, wait 0.2 s, stop.",
			"The last step can be `shooter.runOnce(shooter::stop)`.",
		],
		docs: [DOCS.compositions],
	},
	{
		id: "ready-set-shoot",
		number: 7,
		title: "Ready, Set, Shoot",
		level: "Building commands",
		files: [challengeFile("Challenge07ReadySetShoot")],
		mode: "teleop",
		summary:
			"Hold RB to intake and spin up the shooter at the same time, and shoot the moment the piece is loaded. Letting go of RB stops everything.",
		trial:
			"Holds RB from 0.5 s to 4 s; a piece rolls in at 1.6 s. Then holds RB from 4.5 s to 5 s with no piece around.",
		learn: ["parallel", "deadline", "race", "deadlineFor", "alongWith"],
		idea: [
			"Groups run commands **at the same time**. `Commands.parallel(a, b)` ends when all of them have finished, `Commands.race(a, b)` ends when the first one does, and `Commands.deadline(main, others...)` ends when `main` does.",
			"The same as decorators: `a.alongWith(b)`, `a.raceWith(b)`, and `a.deadlineFor(b)` (run `b` alongside `a`, and stop `b` when `a` ends).",
			"Two commands in a group can't require the same subsystem at the same time. So keep the shooter spinning with **one** command that runs alongside the whole intake → wait → feed sequence.",
		],
		steps: [
			"Build \"intake until loaded\" (challenge 5) and \"feed until the piece is gone\" (challenge 6).",
			"Make a sequence: intake until loaded, wait until at speed, feed, wait 0.2 s.",
			"Run a shooter spin-up command alongside the whole sequence, with `.deadlineFor(...)`, and make the spin-up stop the shooter when it ends.",
			"Bind it to RB with `whileTrue`.",
			ENABLE_TELEOP,
		],
		code: [
			{ code: "sequence.deadlineFor(spinUp)", text: "Runs `spinUp` while `sequence` runs, and ends both when `sequence` ends." },
			{
				code: "shooter.startEnd(() -> shooter.setRPM(Shooter.SHOT_RPM), shooter::stop)",
				text: "Keeps the shooter spinning for as long as it runs.",
			},
		],
		hints: [
			"Watch the timeline: the shooter's bar should start at the same time as the intake's.",
			"`Commands.sequence(intakeUntilLoaded, Commands.waitUntil(shooter::atSpeed), feed, Commands.waitSeconds(0.2)).deadlineFor(spinUp)`",
		],
		docs: [DOCS.compositions],
	},
	{
		id: "whos-in-charge",
		number: 8,
		title: "Who's in Charge?",
		level: "Triggers and interrupts",
		files: [challengeFile("Challenge08WhosInCharge")],
		mode: "teleop",
		summary:
			"The driver taps LB in the middle of a shot, and the shot is lost. Make shots uninterruptible, but let B cancel one on purpose.",
		trial:
			"Pulls RT at 0.5 s and taps LB at 0.9 s, mid-shot. Holds LB from 2.5 s as a piece rolls in. Pulls RT again at 4 s and presses B at 4.4 s.",
		learn: ["interrupt behavior", "cancel", "finallyDo"],
		idea: [
			"When a new command needs a subsystem that's in use, the scheduler normally **cancels** the old command. Shoot and intake both need the indexer, so tapping LB cancels the shot. Look for the interrupted bar on the timeline.",
			"`.withInterruptBehavior(Command.InterruptionBehavior.kCancelIncoming)` flips that around: the running command keeps going, and the new one isn't started.",
			"`command.cancel()` stops a command from anywhere, whatever its interrupt behavior. Bind B to `Commands.runOnce(shoot::cancel)`. (A command for B that requires the shooter would be refused, just like LB's.)",
			"When a sequence is cancelled, the steps it hadn't reached never run, including a \"stop the shooter\" step at the end! Put clean-up in `.finallyDo(...)`, which runs however the command ends.",
		],
		steps: [
			"Run the trial first and watch the timeline: see the shot get interrupted?",
			"In `shoot()`, add `.withInterruptBehavior(...)` so nothing can interrupt a shot.",
			"Move the stop into `.finallyDo(() -> { shooter.stop(); indexer.stop(); })`.",
			"Bind B to cancel the shoot command.",
			ENABLE_TELEOP,
		],
		code: [
			{
				code: ".withInterruptBehavior(Command.InterruptionBehavior.kCancelIncoming)",
				text: "New commands that need the same subsystems are refused while this one runs.",
			},
			{ code: "Commands.runOnce(shoot::cancel)", text: "A command that cancels `shoot`. It requires nothing, so it's never refused." },
			{ code: ".finallyDo(() -> { ... })", text: "Runs when the command ends, whether it finished or was cancelled." },
		],
		hints: [
			"`configureBindings` already puts the shoot command in a variable called `shoot`, so you can call `shoot.cancel()`.",
			"If B cancels the shot but the shooter keeps spinning, your clean-up isn't in `finallyDo`.",
		],
		docs: [DOCS.scheduler, DOCS.compositions],
	},
	{
		id: "led-signals",
		number: 9,
		title: "LED Signals",
		level: "Triggers and interrupts",
		files: [challengeFile("Challenge09LedSignals")],
		mode: "teleop",
		summary:
			"Use the LEDs to tell the drive team what's happening: alliance color when empty, green when holding a piece, white when the shooter is at speed.",
		trial:
			"Holds LB as a piece rolls in at 0.8 s. Taps A (spin up) at 2.5 s and B (stop) at 4.5 s, still holding the piece.",
		learn: ["triggers from sensors", "default commands", "and / negate", "rumble"],
		idea: [
			"A trigger doesn't have to be a button: `new Trigger(indexer::hasPiece)` is true whenever the beam break sees a piece, and you can bind commands to it like any button.",
			"The LEDs are a subsystem, so only one LED command runs at a time. Give them a **default command** (the alliance color) for when nothing else needs them.",
			"Here's the catch. When the shooter gets to speed, the white command interrupts the green one. When the shooter stops, the white command ends, but the green one doesn't come back: `whileTrue` only starts a command when its trigger *becomes* true. So the LEDs fall back to the default.",
			"Fix it by giving each color exactly the conditions it needs: green while `hasPiece.and(atSpeed.negate())`.",
		],
		steps: [
			"Make the LEDs' default command show red or blue, from `DriverStation.getAlliance()`.",
			"Make triggers for \"has a piece\" and \"shooter at speed\".",
			"Bind LED commands to them with `whileTrue`, combining triggers so the colors are right in every case.",
			"Bonus: rumble the controller for half a second when a piece loads.",
			ENABLE_TELEOP,
		],
		code: [
			{ code: "Trigger hasPiece = new Trigger(indexer::hasPiece);", text: "A trigger from a sensor." },
			{ code: "leds.run(() -> leds.setColor(Color.kGreen))", text: "Keeps the LEDs green while it runs." },
			{ code: "hasPiece.and(atSpeed.negate())", text: "True when there's a piece and the shooter is *not* at speed." },
			{
				code: "DriverStation.getAlliance()",
				text: "An `Optional<Alliance>`: maybe empty, if the Driver Station hasn't said. Try `.orElse(Alliance.Blue) == Alliance.Red`.",
			},
			{
				code: "controller.setRumble(RumbleType.kBothRumble, 1.0)",
				text: "Rumbles the controller (0 stops it). Use `controller.setRumble`, not `getHID()`, so the lab can see it.",
			},
		],
		hints: [
			"The default command: `leds.setDefaultCommand(leds.run(() -> leds.setColor(DriverStation.getAlliance().orElse(Alliance.Blue) == Alliance.Red ? Color.kRed : Color.kBlue)));`",
			"Bonus: `hasPiece.onTrue(Commands.startEnd(() -> controller.setRumble(RumbleType.kBothRumble, 1), () -> controller.setRumble(RumbleType.kBothRumble, 0)).withTimeout(0.5));`",
		],
		docs: [DOCS.triggers, DOCS.alliance],
	},
	{
		id: "smart-triggers",
		number: 10,
		title: "Smart Triggers",
		level: "Triggers and interrupts",
		files: [challengeFile("Challenge10SmartTriggers")],
		mode: "teleop",
		summary:
			"RT shoots only when a piece is loaded, and an anti-jam trigger spots a stuck piece from the motor current and clears it.",
		trial:
			"Pulls RT with nothing loaded, intakes a piece and shoots it. Then a piece jams in the intake at 5.2 s, and the driver presses LB again at 6.5 s.",
		learn: ["and / negate", "debounce", "triggers from current"],
		idea: [
			"Combine triggers with `a.and(b)`, `a.or(b)` and `a.negate()`. `controller.rightTrigger().and(hasPiece)` is only true when RT is pulled *and* there's a piece.",
			"A jammed piece makes the roller motor work hard: `intake.getRollerCurrent()` stays above 30 A. But a normal pickup makes it jump for a moment too.",
			"`trigger.debounce(0.25)` is only true once its condition has been true for 0.25 s in a row, so short blips are ignored.",
		],
		steps: [
			"Bind RT, combined with a has-a-piece trigger, to `shoot()`.",
			"Write `unjam()`: run the rollers at `Intake.EJECT_SPEED` for 0.3 s.",
			"Make a trigger that's true when the roller current is above 30 A, debounce it by 0.25 s, and run `unjam()` when it becomes true.",
			"Bonus: RT with nothing loaded flashes the LEDs red for half a second, then turns them off.",
			ENABLE_TELEOP,
		],
		code: [
			{ code: "new Trigger(() -> intake.getRollerCurrent() > 30).debounce(0.25)", text: "True when the current has been high for a quarter of a second." },
			{
				code: "intake.startEnd(() -> intake.setRollers(Intake.EJECT_SPEED), intake::stopRollers)\n    .withTimeout(0.3)",
				text: "Runs the rollers backwards for 0.3 s.",
			},
			{ code: "controller.rightTrigger().and(hasPiece.negate())", text: "RT with nothing loaded." },
		],
		hints: [
			"Unjamming interrupts the intake command (they both need the intake). That's fine: the driver presses LB again.",
			"Bonus: `leds.startEnd(() -> leds.setColor(Color.kRed), leds::off).withTimeout(0.5)`",
		],
		docs: [DOCS.triggers],
	},
	{
		id: "climber",
		number: 11,
		title: "Write a Subsystem",
		level: "Your own subsystems and commands",
		files: ["src/main/java/frc/robot/subsystems/Climber.java", challengeFile("Challenge11Climber")],
		mode: "teleop",
		summary:
			"Write the climber subsystem and its commands: up on the d-pad raises the hook to the top, down pulls the robot up off the floor.",
		trial:
			"Presses up at 0.5 s and down at 1 s (part-way up). Then up at 2 s, holds up at 4 s (already at the top), and presses down at 5 s to climb.",
		learn: ["writing a subsystem", "command factories", "until", "safety limits"],
		idea: [
			"A subsystem **owns its hardware**. `Climber` makes its `ClimberMotor`, and nothing else touches it. The rest of the robot uses the climber only through the commands it offers, like `climber.extend()`.",
			"Methods that make commands are called **command factories**. Made with the subsystem's own `run(...)`, `runEnd(...)` and so on, they all require the climber. So pressing down while the hook is going up interrupts `extend()`, and the hook reverses straight away.",
			"Safety belongs in the subsystem: stop at `TOP` and at the bottom limit switch, so no button combination can drive the motor into a hard stop.",
		],
		steps: [
			"In `Climber.java`, write `extend()`: run the motor at `EXTEND_VOLTS` until the height reaches `TOP`, then stop.",
			"Write `climb()`: run at `CLIMB_VOLTS` until `motor.isAtBottom()`, then stop.",
			"In the challenge, bind `controller.povUp()` and `controller.povDown()` with `onTrue`.",
			ENABLE_TELEOP,
			"Watch the side view: the hook hooks onto the bar and lifts the robot.",
		],
		code: [
			{
				code: "run(() -> motor.setVoltage(EXTEND_VOLTS))\n    .until(() -> motor.getHeight() >= TOP)\n    .finallyDo(motor::stop)\n    .withName(\"Extend\")",
				text: "Inside a subsystem, `run(...)` is the subsystem's own: it makes a command that requires the climber.",
			},
			{ code: "motor.isAtBottom()", text: "The bottom limit switch." },
			{ code: "controller.povUp()  controller.povDown()", text: "The d-pad." },
		],
		hints: [
			"`onTrue` is right here: each command ends by itself when it gets where it's going.",
			"Without `.until(...)`, extend keeps pushing into the hard stop.",
		],
		docs: [DOCS.subsystems, DOCS.organizing],
	},
	{
		id: "aim-at-hub",
		number: 12,
		title: "Write a Command Class",
		level: "Your own subsystems and commands",
		files: ["src/main/java/frc/robot/commands/AimAtHub.java", challengeFile("Challenge12AimAtHub")],
		mode: "teleop",
		summary:
			"While LT is held, turn the robot to face the hub and keep it there, while the driver still drives around with the left stick.",
		trial:
			"The robot starts facing away from the hub. Holds LT from 0.5 s to 3.5 s, pushing the left stick sideways part of the time. Then turns with the right stick.",
		learn: ["Command classes", "addRequirements", "PIDController", "continuous input", "interrupting a default command"],
		idea: [
			"Besides factories like `run(...)`, you can write a whole class that `extends Command`. The scheduler calls `initialize()` once, `execute()` every loop, `isFinished()` after each execute, and `end(interrupted)` once at the end.",
			"A command class must say what it needs: `addRequirements(drive)` in its constructor. Because it requires the drive, holding LT **interrupts** the default driving command, which comes back when LT is let go.",
			"A `PIDController` turns the heading error into a turning speed. Headings wrap around (179° is right next to -179°), so call `enableContinuousInput(-Math.PI, Math.PI)` to always turn the short way.",
			"Tune it: too little kP and the robot lags behind as it drives sideways; too much and it overshoots. A little kD calms it down.",
		],
		steps: [
			"In `AimAtHub`, add `addRequirements(drive)` to the constructor and make a `PIDController`.",
			"In `execute()`, calculate a turning speed from `drive.getRotation()` and `drive.getAngleToHub()`, then call `drive.driveFieldRelative(xSpeed.getAsDouble(), ySpeed.getAsDouble(), turn)`.",
			"In `end()`, stop the robot.",
			ENABLE_TELEOP,
		],
		code: [
			{ code: "private final PIDController pid = new PIDController(kP, 0, kD);", text: "Try kP around 10 and kD around 0.5 to start." },
			{ code: "pid.enableContinuousInput(-Math.PI, Math.PI);", text: "Angles in radians wrap around at ±π." },
			{
				code: "pid.calculate(drive.getRotation().getRadians(), drive.getAngleToHub().getRadians())",
				text: "The turning speed, in rad/s. Measurement first, then the goal.",
			},
			{ code: "xSpeed.getAsDouble()", text: "Reads a `DoubleSupplier`: the driver's speed right now." },
		],
		hints: [
			"Clamp the turning speed with `MathUtil.clamp(turn, -Drive.MAX_ANGULAR_SPEED, Drive.MAX_ANGULAR_SPEED)`.",
			"If the robot doesn't move at all when LT is held, check `addRequirements(drive)`: without it, the default command keeps driving at the same time.",
			"kP = 12 and kD = 0.8 pass the trial.",
		],
		docs: [DOCS.commands, DOCS.pid],
	},
	{
		id: "timed-auto",
		number: 13,
		title: "Timed Autonomous",
		level: "Autonomous",
		files: [challengeFile("Challenge13TimedAuto")],
		mode: "auto",
		summary:
			"Write a 15-second autonomous routine: shoot the preloaded piece, drive at least 1 m, and stop. Bonus: grab the piece in front of you and score it too.",
		trial:
			"Runs your autonomous command for 15 s. The robot starts at the shooting spot with a piece; another piece is 1.7 m in front of it.",
		learn: ["getAutonomousCommand", "withTimeout", "a whole routine as one command"],
		idea: [
			"For the first 15 seconds of a match, the robot runs one command on its own: the one `getAutonomousCommand()` returns. The lab schedules it when the robot is enabled in Autonomous.",
			"A whole routine is one big `Commands.sequence(...)`, built from the same pieces as teleop: shoot, intake, drive.",
			"`.withTimeout(seconds)` ends a command after a while: good enough for \"drive forward for 1.2 s\". Drive motors keep their last speed, so stop the robot when you're done.",
		],
		steps: [
			"Copy your shoot command from challenge 6 (or 8, with `finallyDo`).",
			"Return a sequence: shoot, drive for a while, stop.",
			"Bonus: drive forward over the piece while intaking (`Commands.deadline(driveForward, intakeUntilLoaded)`), drive back, and shoot again.",
			ENABLE_AUTO,
		],
		code: [
			{ code: "drive.run(() -> drive.driveRobotRelative(1.0, 0, 0)).withTimeout(1.3)", text: "Drives forward at 1 m/s for 1.3 s." },
			{ code: "drive.runOnce(drive::stop)", text: "Stops the robot." },
			{ code: SHOOT_CODE, text: "A shoot command." },
		],
		hints: [
			"A command object can only be used once in a sequence. Make two shoot commands by calling your `shoot()` method twice.",
			"Driving forward 1.3 s at 1 m/s puts the piece right in your intake.",
		],
		docs: [DOCS.compositions, DOCS.structuring],
	},
	{
		id: "pathplanner-auto",
		number: 14,
		title: "PathPlanner Autonomous",
		level: "Autonomous",
		files: [challengeFile("Challenge14PathPlannerAuto"), "src/main/deploy/pathplanner/autos/Two Piece.auto"],
		mode: "auto",
		summary:
			"Register named commands, add them to the \"Two Piece\" auto in the PathPlanner tab, and score 2 pieces in autonomous. Bonus: a third.",
		trial:
			"Runs your autonomous command for 15 s. The robot starts at the shooting spot with a piece; pieces wait at the top (2.9, 5.9) and bottom (2.9, 2.17).",
		learn: ["PathPlanner", "named commands", "AutoBuilder", "event markers"],
		idea: [
			"PathPlanner lets you draw where the robot drives, and follows the path for you. The lab has already set up PathPlanner's `AutoBuilder` with the drive (see `CommandLab.java`).",
			"A PathPlanner **auto** is a list of commands, just like your code: paths, waits, groups (sequential, parallel, deadline, race) and **named commands**: commands from your code, registered by name.",
			"Register named commands with `NamedCommands.registerCommand(\"Shoot\", shoot())` *before* building the auto: here, in the challenge's constructor.",
			"To run a command while a path drives, put the path and the command in a **deadline group**, or add an **event marker** to the path.",
		],
		steps: [
			"In the constructor, register \"Intake\" (intake until loaded, with a 3 s timeout) and \"Shoot\".",
			"Open the **PathPlanner** tab and the auto **Two Piece**. Add the named command Shoot at the start, put \"Start to Top Piece\" in a deadline group with Intake, and add Shoot at the end. It saves by itself.",
			"Return `AutoBuilder.buildAuto(\"Two Piece\")` from `getAutonomousCommand()`.",
			ENABLE_AUTO,
			"Bonus: make paths to the bottom piece and a \"Three Piece\" auto.",
		],
		code: [
			{ code: 'NamedCommands.registerCommand("Shoot", shoot());', text: "Makes a command available to PathPlanner autos by name. Names must match exactly." },
			{ code: 'AutoBuilder.buildAuto("Two Piece")', text: "Builds the auto saved in `deploy/pathplanner/autos/Two Piece.auto`." },
			{ code: INTAKE_CODE + "\n  .withTimeout(3)", text: "An intake command for autos: it gives up after 3 s." },
		],
		hints: [
			"After changing an auto in PathPlanner, click **Restart** in the Driver Station so the robot program reads it again.",
			"If the robot drives but never shoots, check the auto uses your named commands, and that the names match what you registered.",
			"For the bottom piece, end a path at about (2.9, 2.7) facing -90°, so the intake drives into the piece.",
		],
		docs: [DOCS.ppBuildAuto, DOCS.ppNamed, DOCS.ppGui],
	},
	{
		id: "match-play",
		number: 15,
		title: "Match Play",
		level: "Capstone",
		files: [challengeFile("Challenge15MatchPlay")],
		mode: "teleop",
		summary:
			"Put it all together: write the controls you'd want in a real match, then drive a one-minute match yourself. Score 3 to pass, 6 for a star, and hang at the end for another.",
		trial:
			"You drive, with the keyboard, a gamepad, or the on-screen controller! A 60-second match with pieces on the field; the human player rolls in a new one near your driver station every few seconds.",
		learn: ["putting it all together", "driver controls"],
		idea: [
			"Real robot code is made of the pieces you've written: a default drive command, intake and shoot commands, triggers that make the controls smart, and LEDs that tell the driver what's happening.",
			"Plan your controls before you write them: which button does what? Hold or press? What should happen if the driver presses two at once?",
			"Shoot from anywhere: aim at the hub with your `AimAtHub` and set the speed from the distance with `Shooter.rpmForDistance(drive.getDistanceToHub())`.",
		],
		steps: [
			"Copy the commands you need from your earlier challenges into this file.",
			"Bind them to buttons, and give the drive a default command.",
			"Click **Free play** to practise. **+ Piece** and **Reset** help.",
			"When you're ready: **Run trial**, then Enable in Teleop. Go!",
		],
		code: [
			{ code: "Shooter.rpmForDistance(drive.getDistanceToHub())", text: "The speed that lands a piece in the hub from where you are." },
			{ code: "new Climber()", text: "Your climber from challenge 11. The tower is in the top-left corner of the field." },
		],
		hints: [
			"No gamepad? Click this tab and drive with the keyboard: **W A S D** to move, **← →** to turn, **Shift** for fine control. Each button's key is drawn on the controller (A is **K**, LB is **E**, RT is **O**...). Practise in free play first.",
			"The fastest cycle is often: intake and spin up at the same time (challenge 7), then aim and shoot.",
		],
		docs: [DOCS.structuring, DOCS.organizing],
	},
];

export function findChallenge(id: string): Challenge | undefined {
	return CHALLENGES.find((challenge) => challenge.id === id);
}

export function optionLabel(challenge: Challenge): string {
	return challenge.number === 0 ? challenge.title : `${challenge.number}. ${challenge.title}`;
}
