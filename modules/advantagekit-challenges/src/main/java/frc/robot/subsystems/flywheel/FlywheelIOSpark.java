package frc.robot.subsystems.flywheel;

/**
 * The flywheel's hardware layer on the real robot, with a Spark MAX motor controller.
 *
 * <p>This lesson's robot is simulated, so this class doesn't do anything here. It shows where your
 * vendor library calls go: {@code spark.getEncoder().getVelocity()} in {@code updateInputs}, and
 * {@code spark.setVoltage(volts)} in {@code setVoltage}. No other class ever sees them.
 */
public class FlywheelIOSpark implements FlywheelIO {
  @Override
  public void updateInputs(FlywheelIOInputs inputs) {
    // inputs.velocityRadPerSec =
    //     Units.rotationsPerMinuteToRadiansPerSecond(spark.getEncoder().getVelocity());
    // inputs.appliedVolts = spark.getAppliedOutput() * spark.getBusVoltage();
    // inputs.currentAmps = spark.getOutputCurrent();
  }

  @Override
  public void setVoltage(double volts) {
    // spark.setVoltage(volts);
  }
}
