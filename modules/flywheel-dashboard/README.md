# Flywheel Tuning

Work in `src/main/java/frc/robot/RobotContainer.java`. Run this lesson with the CodeRunner Driver Station, and open the **Flywheel Tuning** tab next to AdvantageScope.

The robot simulates a flywheel. The **Flywheel Tuning** dashboard sends it a target speed and controller gains through NetworkTables, and shows the speed, voltage and a speed history chart as the robot runs.

## Steps

1. Click **Start** in the Driver Station, then enable **Teleop**. Watch the flywheel spin up on the dashboard.
2. Move the **kP** slider. What happens to the speed when kP is small? When it is large? Why does the flywheel never quite reach the target?
3. Add a feedforward term: in `robotPeriodic()`, add `kV * targetRpm` to `volts`. Click **Restart**, set kP to 0, and find the kV that holds the target on its own. Then bring kP back up.
4. Log `"Flywheel/AtSpeed"` as `true` when the flywheel is within 100 RPM of the target. The dashboard's **At speed** card lights up as soon as you publish it.

## Bonus

- Change the target with the slider while enabled. How quickly does the flywheel recover with P only, versus with feedforward?
- Log the error (`targetRpm - velocityRpm`) and chart it in AdvantageScope.
