package frc.robot.subsystems;

import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lab.Hardware;
import org.littletonrobotics.junction.Logger;

/**
 * The LED strips around the robot. Drive teams read them from across the field, so they're a good
 * way to show what the robot is doing: has it got a piece, is the shooter ready, and so on.
 *
 * <p>Colors come from WPILib's {@link Color} class, for example {@code Color.kGreen}.
 */
public class Leds extends SubsystemBase {
  @Override
  public void periodic() {
    Logger.recordOutput("Leds/Color", getColor().toHexString());
  }

  /** Lights every LED in this color. */
  public void setColor(Color color) {
    Hardware.setLeds(color);
  }

  /** Turns the LEDs off. */
  public void off() {
    Hardware.setLeds(Color.kBlack);
  }

  /** The color the LEDs are showing. */
  public Color getColor() {
    return Hardware.leds();
  }
}
