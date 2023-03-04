// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import edu.wpi.first.wpilibj2.command.WaitUntilCommand;
import frc.robot.Constants.IntakeConstants;
import frc.robot.subsystems.Altitude;
import frc.robot.subsystems.Extension;
import frc.robot.subsystems.Intake;

public class ScoreHighCube extends SequentialCommandGroup {
  public ScoreHighCube(
      Altitude m_altitude,
      Extension m_extension,
      Intake m_intake) {
    addCommands(
        // Prepare High Drop off Cube (move extension and altitude to proper position)
        new PrepareHighCubeDropOff(m_extension, m_altitude),
        new WaitUntilCommand(() -> m_extension.ExtensionIsInHighCubeShootPosition()),
        new WaitUntilCommand(() -> m_altitude.AltitudeIsInHighCubeShootPosition()),
        // Run Eject Cube
        new ScoreCube(m_altitude, m_extension, m_intake),
        // Return to Travel Position
        new PrepareTravelAfterScoring(m_extension, m_altitude));
  }
}
