/*
 * Copyright (c) 2017-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

/**
 * Enumeration of belt vibration patterns.
 */
public enum BeltVibrationPattern {

    NO_VIBRATION((byte)0, false),
    CONTINUOUS((byte)1, false),
    SINGLE_SHORT_PULSE((byte)2, false),
    SINGLE_LONG_PULSE((byte)3, false),
    DOUBLE_SHORT_PULSE((byte)4, false),
    DOUBLE_LONG_PULSE((byte)5, false),
    WARNING((byte)6, true),
    CIRCULAR_RIGHT((byte)7, true),
    FADE_OUT((byte)8, false),
    DESTINATION_REACHED((byte)9, true),
    FRONT_CROSS((byte)10, true),
    BATTERY_LEVEL_0((byte)11, true),
    BATTERY_LEVEL_1((byte)12, true),
    BATTERY_LEVEL_2((byte)13, true),
    BATTERY_LEVEL_3((byte)14, true),
    BATTERY_LEVEL_4((byte)15, true),
    BATTERY_LEVEL_5((byte)16, true),
    BATTERY_LEVEL_6((byte)17, true),
    BATTERY_LEVEL_7((byte)18, true),
    BATTERY_LEVEL_8((byte)19, true),
    BATTERY_LEVEL_9((byte)20, true),
    BATTERY_LEVEL_10((byte)21, true),
    BATTERY_LEVEL_11((byte)22, true),
    BATTERY_LEVEL_12((byte)23, true),
    BATTERY_LEVEL_13((byte)24, true),
    BATTERY_LEVEL_14((byte)24, true),
    BATTERY_LEVEL_15((byte)24, true);

    // Value of the item
    private byte value;

    // Indicates that the pattern is based on binary mask
    private boolean isBinaryMask;

    /**
     * Constructor of enum items.
     * @param v The value of the item.
     */
    BeltVibrationPattern(byte v, boolean isBinaryMask) {
        value = v;
        this.isBinaryMask = isBinaryMask;
    }

    /**
     * Returns the value of the item.
     *
     * @return The value of the item.
     */
    public byte getValue() {
        return value;
    }

    /**
     * Returns an item from its value.
     *
     * @param v The value of the item to retrieve.
     * @param defaultItem The default item when the value does not match any item.
     * @return The item corresponding to the value or the default item.
     */
    static BeltVibrationPattern fromValue(byte v, BeltVibrationPattern defaultItem) {
        for (BeltVibrationPattern item: BeltVibrationPattern.values()) {
            if (item.value == v) {
                return item;
            }
        }
        return defaultItem;
    }


    /**
     * Indicates if the vibration pattern is based on binary mask and cannot be combined with binary
     * mask orientation type.
     *
     * @return <code>true</code> if the pattern is based on binary mask.
     */
    boolean isBinaryMask() {
        return isBinaryMask;
    }

    @Override
    public String toString() {
        switch (this) {
            case NO_VIBRATION:
                return "No vibration";
            case CONTINUOUS:
                return "Continuous";
            case SINGLE_SHORT_PULSE:
                return "Single short pulse";
            case SINGLE_LONG_PULSE:
                return "Single long pulse";
            case DOUBLE_SHORT_PULSE:
                return "Double short pulse";
            case DOUBLE_LONG_PULSE:
                return "Double long pulse";
            case WARNING:
                return "Warning";
            case CIRCULAR_RIGHT:
                return "Circular right";
            case FADE_OUT:
                return "Fade-out";
            case DESTINATION_REACHED:
                return "Destination reached";
            case FRONT_CROSS:
                return "Front cross";
            case BATTERY_LEVEL_0:
                return "Battery level 0";
            case BATTERY_LEVEL_1:
                return "Battery level 1";
            case BATTERY_LEVEL_2:
                return "Battery level 2";
            case BATTERY_LEVEL_3:
                return "Battery level 3";
            case BATTERY_LEVEL_4:
                return "Battery level 4";
            case BATTERY_LEVEL_5:
                return "Battery level 5";
            case BATTERY_LEVEL_6:
                return "Battery level 6";
            case BATTERY_LEVEL_7:
                return "Battery level 7";
            case BATTERY_LEVEL_8:
                return "Battery level 8";
            case BATTERY_LEVEL_9:
                return "Battery level 9";
            case BATTERY_LEVEL_10:
                return "Battery level 10";
            case BATTERY_LEVEL_11:
                return "Battery level 11";
            case BATTERY_LEVEL_12:
                return "Battery level 12";
            case BATTERY_LEVEL_13:
                return "Battery level 13";
            case BATTERY_LEVEL_14:
                return "Battery level 14";
            case BATTERY_LEVEL_15:
                return "Battery level 15";
        }
        return "Unknown";
    }

}
