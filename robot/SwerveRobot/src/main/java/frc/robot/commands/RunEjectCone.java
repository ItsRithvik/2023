// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import frc.robot.subsystems.IntakeNoPID;
import frc.robot.Constants.IntakeNoPIDConstants;
import frc.robot.subsystems.Altitude;
import frc.robot.subsystems.Extension;

public class RunEjectCone extends SequentialCommandGroup {
        public RunEjectCone(
                        Altitude m_altitude,
                        Extension m_extension,
                        IntakeNoPID m_intake) {
                addCommands(
                                // 1. Prepare Drop Off Cone (lower altitude slightly)
                                new PrepareDropOffCone(m_altitude),
                                // Run Eject Cone and Return to Travel
                                new ParallelCommandGroup(
                                                // Eject Cone, Wait X time and then turn off intake
                                                new SequentialCommandGroup(
                                                                // 2. Run eject Cone
                                                                new InstantCommand(() -> m_intake.ejectCone()),
                                                                // 3. Wait X sec and then turn off intake
                                                                new WaitCommand(IntakeNoPIDConstants.kEjectWaitTime)
                                                                                .andThen(new InstantCommand(
                                                                                                m_intake::stopIntake))),
                                                // Return to Travel Position
                                                new PrepareTravelAfterScoring(m_extension, m_altitude)));
        }
}