// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.util.WPIUtilJNI;

// import edu.wpi.first.wpilibj.ADIS16448_IMU;
// import edu.wpi.first.wpilibj.ADXRS450_Gyro;
import com.ctre.phoenix.sensors.WPI_Pigeon2;

import frc.robot.Constants.ExtensionConstants;
import frc.robot.Constants.DriveConstants;
import frc.robot.Constants.AltitudeConstants;
import frc.robot.Constants.OIConstants;
import frc.robot.utilities.SwerveUtils;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj.RobotState;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class DriveSubsystem extends SubsystemBase {
  private boolean TUNING_MODE = true;
  private Altitude m_altitude;
  private Extension m_extension;

  // Create MAXSwerveModules
  private final MAXSwerveModule m_frontLeft = new MAXSwerveModule(
      DriveConstants.kFrontLeftDrivingCanId,
      DriveConstants.kFrontLeftTurningCanId,
      DriveConstants.kFrontLeftChassisAngularOffset);

  private final MAXSwerveModule m_frontRight = new MAXSwerveModule(
      DriveConstants.kFrontRightDrivingCanId,
      DriveConstants.kFrontRightTurningCanId,
      DriveConstants.kFrontRightChassisAngularOffset);

  private final MAXSwerveModule m_rearLeft = new MAXSwerveModule(
      DriveConstants.kRearLeftDrivingCanId,
      DriveConstants.kRearLeftTurningCanId,
      DriveConstants.kBackLeftChassisAngularOffset);

  private final MAXSwerveModule m_rearRight = new MAXSwerveModule(
      DriveConstants.kRearRightDrivingCanId,
      DriveConstants.kRearRightTurningCanId,
      DriveConstants.kBackRightChassisAngularOffset);

  // The gyro sensor
  // private final ADIS16448_IMU m_gyro = new ADIS16448_IMU();
  // private final ADXRS450_Gyro m_gyro = new ADXRS450_Gyro();
  private final WPI_Pigeon2 m_gyro = new WPI_Pigeon2(DriveConstants.kGyroDeviceNumber);

  // Slew rate filter variables for controlling lateral acceleration
  private double m_currentRotation = 0.0;
  private double m_currentTranslationDir = 0.0;
  private double m_currentTranslationMag = 0.0;

  private SlewRateLimiter m_magLimiter = new SlewRateLimiter(OIConstants.kMagnitudeSlewRate);
  private SlewRateLimiter m_rotLimiter = new SlewRateLimiter(OIConstants.kRotationalSlewRate);
  private double m_prevTime = WPIUtilJNI.now() * 1e-6;

  // Odometry class for tracking robot pose
  SwerveDriveOdometry m_odometry = new SwerveDriveOdometry(
      DriveConstants.kDriveKinematics,
      Rotation2d.fromDegrees(m_gyro.getAngle()),
      new SwerveModulePosition[] {
          m_frontLeft.getPosition(),
          m_frontRight.getPosition(),
          m_rearLeft.getPosition(),
          m_rearRight.getPosition()
      });

  public double getPitch() {
    return m_gyro.getPitch();
  }

  public void logSwerveStates() {
    // System.out.println("Position FrontLeft: " + m_frontLeft.getPosition());
    // System.out.println("Position FrontRight: " + m_frontRight.getPosition());
    // System.out.println("Position RearLeft: " + m_rearLeft.getPosition());
    // System.out.println("Position RearRight: " + m_rearRight.getPosition());
  }

  /** The log method puts interesting information to the SmartDashboard. */
  public void log() {
    SmartDashboard.putNumber("Match time remaining", getMatchTimeRemaining());
    SmartDashboard.putBoolean("Under 60s", getMatchTimeRemaining() > 60);
    SmartDashboard.putBoolean("Under 30s ", getMatchTimeRemaining() > 30);

    // Things to show only in tuning mode
    if (TUNING_MODE) {
      SmartDashboard.putNumber("POSE X Meters", m_odometry.getPoseMeters().getX());
      SmartDashboard.putNumber("POSE Y Meters", m_odometry.getPoseMeters().getY());

      SmartDashboard.putNumber("Angle", m_gyro.getAngle());
      // SmartDashboard.putNumber("Yaw", m_gyro.getYaw());
      SmartDashboard.putNumber("Pitch", m_gyro.getPitch());
      // SmartDashboard.putNumber("Roll", m_gyro.getRoll());

    }
  }

  /** Creates a new DriveSubsystem. */
  public DriveSubsystem(Altitude Altitude, Extension Extension) {
    m_altitude = Altitude;
    m_extension = Extension;
  }

  @Override
  public void periodic() {
    /** Call log method every loop. */
    log();

    // Update the odometry in the periodic block
    //
    m_odometry.update(
        Rotation2d.fromDegrees(m_gyro.getAngle()),
        new SwerveModulePosition[] {
            m_frontLeft.getPosition(),
            m_frontRight.getPosition(),
            m_rearLeft.getPosition(),
            m_rearRight.getPosition()
        });
  }

  /**
   * Returns the currently-estimated pose of the robot.
   *
   * @return The pose.
   */
  public Pose2d getPose() {
    return m_odometry.getPoseMeters();
  }

  /**
   * Resets the odometry to the specified pose.
   *
   * @param pose The pose to which to set the odometry.
   */
  public void resetOdometry(Pose2d pose) {
    m_odometry.resetPosition(
        Rotation2d.fromDegrees(m_gyro.getAngle()),
        new SwerveModulePosition[] {
            m_frontLeft.getPosition(),
            m_frontRight.getPosition(),
            m_rearLeft.getPosition(),
            m_rearRight.getPosition()
        },
        pose);
  }

  public boolean isWithinSafeDrivingLimits() {
    boolean altitudeInSafeLimit = m_altitude.getCurrentAltitude() > AltitudeConstants.kAltitudeSafeMin;
    boolean extenstionInSafeLimit = m_extension.getCurrentExtensionPosition() < ExtensionConstants.kExtensionSafeMax;

    // If in Auto, it is safe to drive faster
    // But if in Teleop, consider us within safe driving limits only if the altitude
    // and extension are within safe limits
    return RobotState.isAutonomous() || (altitudeInSafeLimit && extenstionInSafeLimit);
  }

  public void stop() {
    drive(false, 0, 0, 0, 0, true, false);
  }

  /**
   * Method to drive the robot using joystick info.
   *
   * @param speedLimit        Whether to fix the speed to a set value
   * @param inputSpeed        Speed of the robot in the x direction (forward).
   * @param forwardDirection
   * @param sidewaysDirection
   * @param rotDirection      Angular rate of the robot.
   * @param fieldRelative     Whether to move relative to the field
   * @param rateLimit         Whether to use rate limiting
   */
  public void drive(
      boolean speedLimit,
      double inputSpeed,
      double forwardDirection,
      double sidewaysDirection,
      double rotDirection,
      boolean fieldRelative,
      boolean rateLimit) {

    // Adjust input based on max speed

    // If we set the speed limit use a lower max speed
    // otherwise the speed value with some max
    double speed = speedLimit || !isWithinSafeDrivingLimits()
        ? inputSpeed * DriveConstants.kMaxLimitedSpeedMetersPerSecond
        : inputSpeed * DriveConstants.kMaxSpeedMetersPerSecond;

    rotDirection = speedLimit || !isWithinSafeDrivingLimits()
        ? rotDirection * DriveConstants.kMaxLimitedAngularSpeed
        : rotDirection * DriveConstants.kMaxAngularSpeed;

    double forwardDirectionCommanded;
    double sidewaysDirectionCommanded;

    if (rateLimit) {
      // Convert XY to polar for rate limiting
      double inputTranslationDir = Math.atan2(sidewaysDirection, forwardDirection);
      double inputTranslationMag = Math.sqrt(Math.pow(forwardDirection, 2) + Math.pow(sidewaysDirection, 2));

      // Calculate the direction slew rate based on an estimate of the lateral
      // acceleration
      double directionSlewRate;
      if (m_currentTranslationMag != 0.0) {
        directionSlewRate = Math.abs(OIConstants.kDirectionSlewRate / m_currentTranslationMag);
      } else {
        directionSlewRate = 500.0; // some high number that means the slew rate is effectively instantaneous
      }

      double currentTime = WPIUtilJNI.now() * 1e-6;
      double elapsedTime = currentTime - m_prevTime;
      double angleDif = SwerveUtils.AngleDifference(inputTranslationDir, m_currentTranslationDir);
      if (angleDif < 0.45 * Math.PI) {
        m_currentTranslationDir = SwerveUtils.StepTowardsCircular(m_currentTranslationDir, inputTranslationDir,
            directionSlewRate * elapsedTime);
        m_currentTranslationMag = m_magLimiter.calculate(inputTranslationMag);
      } else if (angleDif > 0.85 * Math.PI) {
        if (m_currentTranslationMag > 1e-4) { // some small number to avoid floating-point errors with equality
                                              // checking
          // keep currentTranslationDir unchanged
          m_currentTranslationMag = m_magLimiter.calculate(0.0);
        } else {
          m_currentTranslationDir = SwerveUtils.WrapAngle(m_currentTranslationDir + Math.PI);
          m_currentTranslationMag = m_magLimiter.calculate(inputTranslationMag);
        }
      } else {
        m_currentTranslationDir = SwerveUtils.StepTowardsCircular(m_currentTranslationDir, inputTranslationDir,
            directionSlewRate * elapsedTime);
        m_currentTranslationMag = m_magLimiter.calculate(0.0);
      }
      m_prevTime = currentTime;

      forwardDirectionCommanded = m_currentTranslationMag * Math.cos(m_currentTranslationDir);
      sidewaysDirectionCommanded = m_currentTranslationMag * Math.sin(m_currentTranslationDir);
      m_currentRotation = m_rotLimiter.calculate(rotDirection);

    } else {
      forwardDirectionCommanded = forwardDirection;
      sidewaysDirectionCommanded = sidewaysDirection;
      m_currentRotation = rotDirection;
    }

    // Convert the commanded speeds into the correct units for the drivetrain
    double forwardDirectionDelivered = forwardDirectionCommanded * speed;
    double sidewaysDirectionDelivered = sidewaysDirectionCommanded * speed;
    double rotDelivered = m_currentRotation * DriveConstants.kMaxAngularSpeed;

    var swerveModuleStates = DriveConstants.kDriveKinematics.toSwerveModuleStates(
        fieldRelative
            ? ChassisSpeeds.fromFieldRelativeSpeeds(
                forwardDirectionDelivered,
                sidewaysDirectionDelivered,
                rotDelivered, Rotation2d.fromDegrees(-m_gyro.getAngle()))
            : new ChassisSpeeds(
                forwardDirectionDelivered,
                sidewaysDirectionDelivered,
                rotDelivered));
    SwerveDriveKinematics.desaturateWheelSpeeds(
        swerveModuleStates, DriveConstants.kMaxSpeedMetersPerSecond);
    m_frontLeft.setDesiredState(swerveModuleStates[0]);
    m_frontRight.setDesiredState(swerveModuleStates[1]);
    m_rearLeft.setDesiredState(swerveModuleStates[2]);
    m_rearRight.setDesiredState(swerveModuleStates[3]);
  }

  public void turn(double speed) {
    drive(false, 0, 0, 0, speed, true, false);
  }

  /**
   * Sets the wheels into an X formation to prevent movement.
   */
  public void lock() {
    m_frontLeft.setDesiredState(new SwerveModuleState(0, Rotation2d.fromDegrees(45)));
    m_frontRight.setDesiredState(new SwerveModuleState(0, Rotation2d.fromDegrees(-45)));
    m_rearLeft.setDesiredState(new SwerveModuleState(0, Rotation2d.fromDegrees(-45)));
    m_rearRight.setDesiredState(new SwerveModuleState(0, Rotation2d.fromDegrees(45)));
  }

  /**
   * Sets the swerve ModuleStates.
   *
   * @param desiredStates The desired SwerveModule states.
   */
  public void setModuleStates(SwerveModuleState[] desiredStates) {
    SwerveDriveKinematics.desaturateWheelSpeeds(
        desiredStates, DriveConstants.kMaxSpeedMetersPerSecond);
    m_frontLeft.setDesiredState(desiredStates[0]);
    m_frontRight.setDesiredState(desiredStates[1]);
    m_rearLeft.setDesiredState(desiredStates[2]);
    m_rearRight.setDesiredState(desiredStates[3]);
  }

  /** Resets the drive encoders to currently read a position of 0. */
  public void resetEncoders() {
    m_frontLeft.resetEncoders();
    m_rearLeft.resetEncoders();
    m_frontRight.resetEncoders();
    m_rearRight.resetEncoders();
  }

  /** Zeroes the heading of the robot. */
  public void zeroHeading() {
    m_gyro.reset();
  }

  /** Resets the odometry of the robot. */
  public void resetPose() {
    m_gyro.reset();
  }

  /** Calibrates the gyro */
  public void calibrate() {
    m_gyro.calibrate();
  }

  /**
   * Returns the heading of the robot.
   *
   * @return the robot's heading in degrees, from -180 to 180
   */
  public double getHeading() {
    return Rotation2d.fromDegrees(m_gyro.getAngle()).getDegrees();
  }

  /**
   * Returns the turn rate of the robot.
   *
   * @return The turn rate of the robot, in degrees per second
   */
  public double getTurnRate() {
    return m_gyro.getRate() * (DriveConstants.kGyroReversed ? -1.0 : 1.0);
  }

  public double getMatchTimeRemaining() {
    return Timer.getMatchTime();
  }

}