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

public class ScoreMidCube extends SequentialCommandGroup {
        public ScoreMidCube(
                        Altitude m_altitude,
                        Extension m_extension,
                        Intake m_intake) {
                addCommands(
                                // Prepare Drop off Cube (move extension to proper position)
                                new PrepareMidCubeDropOff(m_extension),
                                new WaitUntilCommand(() -> m_extension.ExtensionIsInMidCubeShootPosition())
                                                // Run Eject Cube and Return to Travel
                                                .andThen(new ParallelCommandGroup(
                                                                new InstantCommand(() -> m_intake.ejectCube()),
                                                                // Wait X sec and then turn off intake
                                                                new WaitCommand(IntakeConstants.kEjectWaitTime)
                                                                                .andThen(new InstantCommand(
                                                                                                m_intake::stopIntake)))),
                                // Return to Travel Position
                                new PrepareTravelAfterScoring(m_extension, m_altitude));
        }
}