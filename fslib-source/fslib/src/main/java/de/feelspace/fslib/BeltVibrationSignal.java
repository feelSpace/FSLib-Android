/*
 * Copyright (c) 2017-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */

package de.feelspace.fslib;

/**
 * Enumeration of vibration signals that can be oriented.
 */
public enum BeltVibrationSignal {

    /**
     * Continuous vibration.
     */
    CONTINUOUS(true, true),

    /**
     * Continuous vibration for the navigation.
     */
    NAVIGATION(true, true),

    /**
     * Fast vibration pulse indicating that the destination is nearby. This signal must be oriented
     * to the destination.
     */
    APPROACHING_DESTINATION(true, true),

    /**
     * Fast vibration pulse indicating the direction during a maneuver.
     */
    TURN_ONGOING(true, true),

    /**
     * A vibration signal to indicate a direction for a short period of time.
     */
    DIRECTION_NOTIFICATION(true, false),

    /**
     * A slow vibration pulse to indicate the direction when the next waypoint is far away.
     */
    NEXT_WAYPOINT_LONG_DISTANCE(true, true),

    /**
     * A vibration pulse to indicate the navigation direction when the next waypoint is at medium
     * distance.
     */
    NEXT_WAYPOINT_MEDIUM_DISTANCE(true, true),

    /**
     * A vibration pulse to indicate the navigation direction when the next waypoint is at short
     * distance.
     */
    NEXT_WAYPOINT_SHORT_DISTANCE(true, true),

    /**
     * A fast vibration pulse to indicate the navigation direction when in the close area of a
     * waypoint.
     */
    NEXT_WAYPOINT_AREA_REACHED(true, true),

    /**
     * A non-oriented vibration pattern indicating that the destination has been reached. The signal
     * is repeated indefinitely.
     */
    DESTINATION_REACHED_REPEATED(false, true),

    /**
     * A non-oriented vibration pattern indicating that the destination has been reached. The signal
     * pattern is performed only one time.
     */
    DESTINATION_REACHED_SINGLE(false, false),

    /**
     * A light warning signal to use when an operation is not permitted or cannot be performed. It
     * is recommended to use channel 0 and an intensity of 25 for this signal.
     */
    OPERATION_WARNING(false, false),

    /**
     * A strong warning signal to indicate a critical problem such as location inaccuracy during
     * navigation. It is recommended to use channel 0 for this signal.
     */
    STRONG_WARNING(false, false),

    /**
     * A system signal that indicates with a vibration the level of the belt's battery. Any
     * customization of this signal (intensity, channel and stop other channel flag) will be
     * ignored. The signal use the channel 0, the default intensity and other channels are not
     * stopped when the signal is started.
     */
    BATTERY_LEVEL(false, false);

    /**
     * <code>true</code> if the signal must be used with an orientation.
     */
    private final boolean directional;

    /**
     * <code>true</code> if the signal is a pattern repeated indefinitely.
     */
    private final boolean isRepeated;

    /**
     * Constructor.
     * @param directional <code>true</code> if the signal must be used with an orientation.
     * @param isRepeated <code>true</code> if the signal is repeated indefinitely.
     */
    BeltVibrationSignal(boolean directional, boolean isRepeated) {
        this.directional = directional;
        this.isRepeated = isRepeated;
    }

    /**
     * Indicates if the signal must be used with an orientation.
     *
     * @return <code>true</code> if the signal must be used with an orientation.
     */
    public boolean isDirectional() {
        return directional;
    }

    /**
     * Indicates if the signal is repeated indefinitely.
     *
     * @return <code>true</code> if the signal is repeated indefinitely.
     */
    public boolean isRepeated() {
        return isRepeated;
    }

}
