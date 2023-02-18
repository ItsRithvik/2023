// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

import static frc.robot.Constants.ElevatorConstants;

import java.util.function.DoubleSupplier;
import frc.robot.subsystems.Elevator;
import edu.wpi.first.wpilibj2.command.PIDCommand;

/**
 * Drive the given distance straight (negative values go backwards). Uses a
 * local PID controller to run a simple PID loop that is only enabled while t
 * is command is running. The input is the averaged values of the left and right
 * encoders.
 */
public class MaintainAltitude extends PIDCommand {
  private final Elevator m_elevator;

  static double kP = 0.1;
  static double kI = 0;
  static double kD = 0.001;

  /**
   * Create a new MaintainAltitude command.
   *
   * @param distance The distance to move (degrees)
   */
  public MaintainAltitude(DoubleSupplier altitudeValue, Elevator elevator) {
    super(
        new PIDController(kP, kI, kD),
        elevator::getCurrentAltitudeAngle,
        altitudeValue,
        output -> elevator.maintain(output));

    m_elevator = elevator;
    addRequirements(m_elevator);
    getController().setTolerance(ElevatorConstants.kAltitudePositionTolerance);

    // SmartDashboard.putNumber("Altitude to maintain",
    // altitudeValue.getAsDouble());

  }

  @Override
  public void execute() {
    super.execute();
    SmartDashboard.putNumber("Current altitude", m_elevator.getCurrentAltitude());
    SmartDashboard.putNumber("Current altitude", m_elevator.getCurrentAltitudeAngle());
  }

  // Called just before this Command runs the first time
  @Override
  public void initialize() {
    // Get everything in a safe starting state.
    m_elevator.reset();
    super.initialize();
  }

  // Make this return true when this Command no longer needs to run execute()
  @Override
  public boolean isFinished() {
    return getController().atSetpoint();
  }
}
