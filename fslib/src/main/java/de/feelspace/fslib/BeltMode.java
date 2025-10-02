/*
 * Copyright (c) 2017-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

import androidx.annotation.Nullable;

/**
 * Enumeration of navigation modes.
 */
public enum BeltMode {

    /**
     * The belt is going to be switched off.
     */
    STANDBY((byte)0),

    /**
     * Initial mode of the belt when it is waiting for user or application command.
     */
    WAIT((byte)1),

    /**
     * The belt shows the magnetic North.
     */
    COMPASS((byte)2),

    /**
     * The belt is controlled by the application.
     */
    APP((byte)3),

    /**
     * The vibration is paused.
     */
    PAUSE((byte)4),

    /**
     * Mode for the calibration of the sensors.
     */
    CALIBRATION((byte)5),

    /**
     * Mode for assisting the crossing of a road.
     *
     * IMPORTANT: This mode has been added in firmware version 45. The application must check the
     * firmware version before changing the mode to the Crossing mode.
     */
    CROSSING((byte)6),

    /**
     * The mode of the belt is unknown because it is not connected or the handshake procedure is
     * not completed.
     */
    UNKNOWN((byte)0xFF);

    // Value of the item
    private byte value;

    /**
     * Constructor of enum items.
     * @param v The value of the item.
     */
    BeltMode(byte v) {
        value = v;
    }

    /**
     * Returns the value of the item.
     *
     * @return The value of the item.
     */
    byte getValue() {
        return value;
    }

    /**
     * Returns an item from its value.
     *
     * @param v The value of the item to retrieve.
     * @return The item corresponding to the value or <code>null</code> if the value does not
     * correspond to any item.
     */
    static @Nullable BeltMode fromValue(byte v) {
        for (BeltMode item: BeltMode.values()) {
            if (item.value == v) {
                return item;
            }
        }
        return null;
    }


    @Override
    public String toString() {
        switch (this) {
            case CROSSING:
                return "Crossing";
            case UNKNOWN:
                return "Unknown";
            case STANDBY:
                return "Standby";
            case WAIT:
                return "Wait";
            case COMPASS:
                return "Compass";
            case APP:
                return "App-mode";
            case PAUSE:
                return "Pause";
            case CALIBRATION:
                return "Calibration";
            default:
                return "Unknown";
        }
    }

}
