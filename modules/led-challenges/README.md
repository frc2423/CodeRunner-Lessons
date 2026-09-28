# LED Challenges

Learn to program by making a robot's LED strip light up. Each challenge is its own class in `src/main/java/frc/robot/subsystems/LEDS/`, and you only need to change that one file.

Open the **LED Challenges** tab next to AdvantageScope. Pick a challenge there to see what to do, which file to edit, the code you might need, and hints. It also shows your robot's LEDs next to an animation of the goal.

## Steps

1. Click **Start** in the Driver Station. The dot at the top of the LED Challenges tab turns green when the robot program is running.
2. Pick **Example: Hello, LEDs** and click **Run**. The first three LEDs turn red, green and blue. Open `Challenge00Example.java` to see how.
3. Work through the challenges in order, starting with **1. First Light**. For each one:
   - Write your code in the challenge's file, in its `update` method.
   - Save, then click **Restart** in the Driver Station so the robot program is rebuilt with your code.
   - Click **Run** and compare **Your robot** with the **Goal**. Some challenges tick off checks when your LEDs match.
4. If your code crashes, the tab shows the error and the line it happened on. Fix it and restart.

The LEDs work while the robot is disabled, so you don't need to enable it (except to test **Robot Status**).

## Challenges

| # | Challenge | You'll practise |
| --- | --- | --- |
| 1 | First Light | statements, method calls, counting from 0 |
| 2 | Mixing Colors | RGB colors |
| 3 | Variables | variables and expressions |
| 4 | Fill the Strip | `for` loops |
| 5 | Stripes | `if`/`else`, the `%` operator |
| 6 | Gradient | arithmetic, `double` and `int`, casting |
| 7 | Blink | using time, `boolean` |
| 8 | Moving Dot | fields (remembering things between updates) |
| 9 | Progress Bar | combining loops and conditions |
| 10 | Flag | writing your own methods |
| 11 | Rainbow | HSV colors, math with the loop variable |
| 12 | Color Palette | arrays |
| 13 | Scanner | several fields working together |
| 14 | Twinkle | random numbers |
| 15 | Morse Code | Strings and chars |
| 16 | Cellular Automaton | boolean arrays, copying state |
| 17 | Robot Status | using WPILib's `DriverStation` |
| 18 | Freestyle | your own light show |

## Bonus

- Many challenges have a bonus in their hints: stripes 3 LEDs wide, a fading tail for the scanner, your team name in Morse code, Rule 30.
- Keep more than one light show: write each in its own class that `implements Led`. To run one from the tab, point the `"freestyle"` line in `LedChallenges.java` at it, for example `CHALLENGES.put("freestyle", MyShow::new);`, then pick **Freestyle**.
