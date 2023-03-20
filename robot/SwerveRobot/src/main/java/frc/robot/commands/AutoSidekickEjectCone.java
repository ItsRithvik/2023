// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import frc.robot.Constants.ExtensionConstants;
import frc.robot.subsystems.Altitude;
import frc.robot.subsystems.Extension;
import frc.robot.subsystems.Intake;

import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;

/** An example command that uses an example subsystem. */
public class AutoSidekickEjectCone extends SequentialCommandGroup {
    public AutoSidekickEjectCone(
            Altitude m_altitude,
            Extension m_extension,
            Intake m_intake) {
        addCommands(
                new Extend(m_extension,
                        ExtensionConstants.kExtensionPositionHighDropOff - 5),
                new InstantCommand(() -> m_intake.ejectCone()),
                new Extend(m_extension, ExtensionConstants.kExtensionPositionHighDropOff - 15),
                new InstantCommand(() -> m_intake.stopIntake()));
    }
}
