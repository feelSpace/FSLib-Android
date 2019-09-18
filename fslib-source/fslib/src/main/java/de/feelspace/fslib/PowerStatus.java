/*
 * Copyright (c) 2018-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

/**
 * Enumeration of power status for the belt.
 */
public enum PowerStatus {

    /**
     * Unknown power supply status.
     */
    UNKNOWN((byte)0),

    /**
     * The belt is powered by its internal battery.
     */
    ON_BATTERY((byte)1),

    /**
     * The battery of the belt is charging.
     */
    CHARGING((byte)2),

    /**
     * The battery is full and the belt is powered by an external power source through USB.
     */
    EXTERNAL_POWER((byte)3);

    // Value of the item
    private byte value;

    /**
     * Constructor of enum items.
     * @param v The value of the item.
     */
    PowerStatus(byte v) {
        value = v;
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
    public static PowerStatus fromValue(byte v, PowerStatus defaultItem) {
        for (PowerStatus item: PowerStatus.values()) {
            if (item.value == v) {
                return item;
            }
        }
        return defaultItem;
    }

    @Override
    public String toString() {
        switch (this) {
            case UNKNOWN:
                return "Unknown";
            case ON_BATTERY:
                return "On battery";
            case CHARGING:
                return "Charging";
            case EXTERNAL_POWER:
                return "External power";
            default:
                return "Unknown";
        }
    }

}
