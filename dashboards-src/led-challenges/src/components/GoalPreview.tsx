import { useEffect, useRef, useState } from "react";
import type { Challenge } from "../challenges";
import type { CodeRunnerRobotState } from "../coderunner/types";
import { type GoalRobotState, LedStrip } from "../leds";
import { LedStripView } from "./LedStripView";

function goalRobotState(robot: CodeRunnerRobotState | null): GoalRobotState {
	return {
		enabled: robot?.enabled === true && !robot.eStopped,
		autonomous: robot?.mode === "auto",
		redAlliance: robot?.alliance.startsWith("red") ?? false,
	};
}

/**
 * Plays the challenge's goal the way the robot would run a finished solution:
 * `start` once, then `update` every 20 ms. It restarts whenever `runKey`
 * changes, so it lines up with the student's own run.
 */
export function GoalPreview({
	challenge,
	count,
	robot,
	runKey,
}: {
	challenge: Challenge;
	count: number;
	robot: CodeRunnerRobotState | null;
	runKey: number;
}) {
	const [colors, setColors] = useState<number[]>(() =>
		new Array(count).fill(0),
	);
	// Read on every tick, so a Driver Station change doesn't restart the goal.
	const robotState = useRef(goalRobotState(robot));
	robotState.current = goalRobotState(robot);

	// runKey is only a trigger: a new value restarts the goal.
	useEffect(() => {
		void runKey;
		const program = challenge.goal?.();
		const strip = new LedStrip(count);
		program?.start?.(strip);
		const started = performance.now();
		const tick = () => {
			program?.update(
				strip,
				(performance.now() - started) / 1000,
				robotState.current,
			);
			setColors([...strip.colors]);
		};
		tick();
		const timer = window.setInterval(tick, 20);
		return () => window.clearInterval(timer);
	}, [challenge, count, runKey]);

	return (
		<LedStripView
			colors={challenge.goal ? colors : undefined}
			count={count}
			overlay={challenge.goal ? undefined : "No goal: this one is up to you!"}
		/>
	);
}
