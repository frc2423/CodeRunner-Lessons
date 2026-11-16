# Smart Steering Playground

In Mario Kart, smart steering keeps you on the track: you still drive, but the game quietly nudges you away from the edges. In this playground you write the same thing for a swerve robot. The driver drives with a gamepad or the keyboard, and your code makes sure the robot never crashes into an obstacle or a field wall, without taking the driving away from them.

Open the **Playground** tab next to AdvantageScope. It shows the field from above: the robot (the arrow shows which way it faces), the obstacles, what the driver asked for (dashed arrow) and what your steering code really drove (green arrow).

Nothing on the field is solid: if your smart steering doesn't keep the robot out of an obstacle, it drives straight through it. That way you can always see when your steering didn't work. (The robot does stay on the field.)

Your code goes in `src/main/java/frc/robot/SmartSteering.java`. Every 20 ms the robot calls its `steer` method with:

| Parameter | What it is |
| --- | --- |
| `requested` | The `ChassisSpeeds` the driver is asking for, field-relative |
| `robotPose` | Where the robot is, and which way it faces |
| `currentSpeeds` | How fast the robot is really moving, field-relative (it can't change speed instantly) |
| `obstacles` | Every obstacle as a `Rectangle2d`, including the four field walls |

Whatever `steer` returns is what the robot drives at. Right now it returns exactly what the driver asked for, so the robot drives straight through everything.

## Steps

1. Click **Start** in the Driver Station. The dot at the top of the Playground tab turns green when the robot program is running.
2. Set the Driver Station to **Teleop** and click **Enable**. Then drive:

   | Keys (click the Playground tab first) | Gamepad | Does |
   | --- | --- | --- |
   | W A S D | left stick | drive |
   | ← → | right stick | spin |
   | Shift (held) | | half speed |

   To use a gamepad, plug it in and pick it in the Driver Station's **Controls**. Driving is field-relative: W (or up on the stick) always drives away from the blue driver station, to the right on the tab, whichever way the robot faces.
3. Drive through some obstacles: without smart steering, nothing stops you.
4. Add obstacles: drag on the field to draw one. Drag a drawn obstacle to move it, or click it to rotate or delete it. Obstacles written in code go in `Obstacles.java`; **Copy as code** turns the ones you drew into lines you can paste there.
5. Write your smart steering in `SmartSteering.java`. Save, then click **Restart** in the Driver Station to run your new code. Some ideas, from simplest to smartest:
   1. **Look ahead.** Where will the robot be in a moment if it keeps going? Make a `Rectangle2d` for the robot there, draw it with `Debug.rectangle`, and work out whether it overlaps any obstacle.
   2. **Brake** before the robot would hit something.
   3. **Slide.** Only remove the part of the speed that points *into* the obstacle. Drive diagonally into a wall: the robot should glide along it instead of stopping.
   4. **Be smooth.** From `d` meters away, the fastest speed the robot can still stop from is `Math.sqrt(2 * MAX_ACCELERATION * d)`.
   5. **Watch the corners.** Spinning swings the robot's corners out, and the robot can't change speed instantly, so use `currentSpeeds`.
6. Use the **Smart steering** switch on the tab to turn your code off and on, and compare. Good smart steering never lets the robot into an obstacle, changes nothing when the robot is far from obstacles, and never stops the driver from driving away from a wall.
7. If your code crashes, the tab shows the error and the line it happened on, and the robot drives without your code until you fix it and restart.

## Tools you can use

- `PlaygroundConstants`: the field size, the robot size (`ROBOT_LENGTH`, `ROBOT_WIDTH`), `MAX_SPEED`, `MAX_ACCELERATION`, and more.
- WPILib's `Rectangle2d`, `Pose2d` and `Translation2d`. Any other math (like whether two rectangles overlap) is yours to write.
- `Debug.line`, `Debug.arrow`, `Debug.point`, `Debug.rectangle` and `Debug.circle` draw on the tab's field, and `Debug.value("name", value)` shows a number next to it. Draw what your code is thinking!

## Bonus

- **Steer.** Like in Mario Kart, gently turn the robot around an obstacle in its way instead of only braking or sliding.
- Draw a narrow corridor and drive through it at full speed without ever touching the sides.
- Make it work for rotated obstacles too: add some with `new Rectangle2d(new Pose2d(x, y, Rotation2d.fromDegrees(30)), xWidth, yWidth)`.
- Everything is logged, so you can study a run in AdvantageScope: put `Playground/RobotPose` on a 2D field, `Playground/ModuleStates` on a swerve tab, and graph `Playground/RequestedSpeeds` against `Playground/SteeredSpeeds`.
