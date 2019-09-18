/*
 * Copyright (c) 2018-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

import android.support.annotation.Nullable;

/**
 * Belt's orientation information.
 */
public class BeltOrientation {

    /** ID of the orientation source. */
    private int sourceId;

    /** Heading of the belt, adjusted with heading offset. */
    private int beltHeading;

    /** Heading of the control box. */
    private @Nullable Integer controlBoxHeading;

    /** Roll of the control box. */
    private @Nullable Integer controlBoxRoll;

    /** Pitch of the control box.  */
    private @Nullable Integer controlBoxPitch;

    /** Accuracy of the orientation. */
    private @Nullable Integer accuracy;

    /** Status of the magnetometer. */
    private @Nullable Integer magnetometerStatus;

    /** Status of the accelerometer. */
    private @Nullable Integer accelerometerStatus;

    /** Status of the gyroscope. */
    private @Nullable Integer gyroscopeStatus;

    /** Status of the fusion. */
    private @Nullable Integer fusionStatus;

    /** Accuracy flag. */
    private @Nullable Boolean inaccurateOrientation;

    /**
     * Constructor.
     */
    protected BeltOrientation(int sourceId, int beltHeading, @Nullable Integer controlBoxHeading,
                              @Nullable Integer controlBoxRoll, @Nullable Integer controlBoxPitch,
                              @Nullable Integer accuracy, @Nullable Integer magnetometerStatus,
                              @Nullable Integer accelerometerStatus,
                              @Nullable Integer gyroscopeStatus, @Nullable Integer fusionStatus,
                              @Nullable Boolean inaccurateOrientation) {
        this.sourceId = sourceId;
        this.beltHeading = beltHeading;
        this.controlBoxHeading = controlBoxHeading;
        this.controlBoxRoll = controlBoxRoll;
        this.controlBoxPitch = controlBoxPitch;
        this.accuracy = accuracy;
        this.magnetometerStatus = magnetometerStatus;
        this.accelerometerStatus = accelerometerStatus;
        this.gyroscopeStatus = gyroscopeStatus;
        this.fusionStatus = fusionStatus;
        this.inaccurateOrientation = inaccurateOrientation;
    }

    /**
     * Returns the ID of the orientation source.
     *
     * @return The source ID.
     */
    protected int getSourceId() {
        return sourceId;
    }

    /**
     * Returns the heading orientation of the belt in degrees. The heading of the belt take into
     * account the heading offset.
     *
     * @return the heading orientation of the belt in degrees.
     */
    public int getBeltHeading() {
        return beltHeading;
    }

    /**
     * Returns the heading of the control box in degrees.
     *
     * @return the heading of the control box in degrees, or <code>null</code> if unknown.
     */
    public Integer getControlBoxHeading() {
        return controlBoxHeading;
    }

    /**
     * Returns the pitch of the control box in degrees.
     *
     * @return the pitch of the control box in degrees, or <code>null</code> if unknown.
     */
    public Integer getControlBoxPitch() {
        return controlBoxPitch;
    }

    /**
     * Returns the roll of the control box in degrees.
     *
     * @return the roll of the control box in degrees, or <code>null</code> if unknown.
     */
    public Integer getControlBoxRoll() {
        return controlBoxRoll;
    }

    /**
     * Returns the accuracy of the orientation in degrees.
     *
     * @return the accuracy of the orientation in degrees, or <code>null</code> if unknown.
     */
    public Integer getAccuracy() {
        return accuracy;
    }

    /**
     * Returns the status of the magnetometer. <code>0</code> for inaccurate or not calibrated, to
     * <code>3</code> for accurate. <code>-1</code> if unknown.
     *
     * @return the status of the magnetometer, or <code>null</code> if unknown.
     */
    public Integer getMagnetometerStatus() {
        return magnetometerStatus;
    }

    /**
     * Returns the status of the accelerometer. <code>0</code> for inaccurate or not calibrated, to
     * <code>3</code> for accurate. <code>-1</code> if unknown.
     *
     * @return the status of the accelerometer, or <code>null</code> if unknown.
     */
    protected Integer getAccelerometerStatus() {
        return accelerometerStatus;
    }

    /**
     * Returns the status of the gyroscope. <code>0</code> for inaccurate or not calibrated, to
     * <code>3</code> for accurate. <code>-1</code> if unknown.
     *
     * @return the status of the gyroscope, or <code>null</code> if unknown.
     */
    protected Integer getGyroscopeStatus() {
        return gyroscopeStatus;
    }

    /**
     * Returns the status of the fusion. <code>0</code> for inaccurate or not calibrated, to
     * <code>3</code> for accurate. <code>-1</code> if unknown.
     *
     * @return the status of the gyroscope, or <code>null</code> if unknown.
     */
    public Integer getFusionStatus() {
        return fusionStatus;
    }

    /**
     * Returns a flag that indicates if the orientation is accurate.
     *
     * @return <code>true</code> if the orientation is accurate, or <code>null</code> if unknown.
     */
    public Boolean isOrientationAccurate() {
        return (inaccurateOrientation==null)?(null):(!inaccurateOrientation);
    }
}
